import * as cdk from 'aws-cdk-lib';
import { Code, Function, Runtime } from 'aws-cdk-lib/aws-lambda';
import { Construct } from 'constructs';
import * as path from 'path';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb'
import * as apigateway from 'aws-cdk-lib/aws-apigateway'
import * as cloudtrail from 'aws-cdk-lib/aws-cloudtrail';
import { Effect, ManagedPolicy, PolicyDocument, PolicyStatement, Role, ServicePrincipal } from 'aws-cdk-lib/aws-iam';
import { LogGroup, LogRetention, RetentionDays } from 'aws-cdk-lib/aws-logs';
import { CfnBudget } from 'aws-cdk-lib/aws-budgets';

export const TELEGRAM_CIDR = [
  '149.154.160.0/20',
  '91.108.4.0/22'
];

export class CdkStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const table = new dynamodb.Table(this, 'BudgetTable', {
      partitionKey: { name: 'id', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'timestamp', type: dynamodb.AttributeType.NUMBER }, // Using number for Unix timestamp
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
    });

    table.addGlobalSecondaryIndex({
      indexName: 'TimestampIndex',
      partitionKey: { name: 'timestamp', type: dynamodb.AttributeType.NUMBER },
    });

    const botLambda = new Function(this, 'BudgetBot', {
      runtime: Runtime.JAVA_11,
      handler: 'com.parable.App::handleRequest',
      code: Code.fromAsset(path.join(__dirname, '../../JavaLambda/build/libs/JavaLambda-1.0-SNAPSHOT-all.jar')),
      memorySize: 1024,
      timeout: cdk.Duration.seconds(25),
      environment: {
        TABLE_NAME: table.tableName,
        BOT_TOKEN: process.env.BOT_TOKEN ?? "N/A",
        CHAT_ID: process.env.CHAT_ID ?? "N/A"
      }
    })

    botLambda._logRetention = new LogRetention(this, "LambdaLogRetention", {
      logGroupName: botLambda.logGroup.logGroupName,
      retention: RetentionDays.ONE_WEEK
    });

    botLambda.addToRolePolicy(new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        'dynamodb:*'
      ],
      resources: [table.tableArn],
    }));

    botLambda.addToRolePolicy(new PolicyStatement({
      actions: ['bedrock:*'],
      resources: ['*']  // TODO: limit this to specific models or use Arn for better security
    }));

    const apiAccessLogs = new LogGroup(this, 'ApiGatewayAccessLogsProd', {
      logGroupName: '/aws/apigateway/access-logs',
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      retention: RetentionDays.ONE_WEEK
    });

    const apiGatewayRole = new Role(this, 'APIGatewayCloudWatchRole', {
      assumedBy: new ServicePrincipal('apigateway.amazonaws.com'),
      roleName: 'APIGatewayCloudWatchRole',
      managedPolicies:[ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonAPIGatewayPushToCloudWatchLogs')]
    });

    new apigateway.CfnAccount(this, 'APIGatewayAccount', {
      cloudWatchRoleArn: apiGatewayRole.roleArn,
    });

    apiAccessLogs.grantWrite(new ServicePrincipal('apigateway.amazonaws.com'))

    const api = new apigateway.RestApi(this, 'BudgetBotAPI', {
      cloudWatchRole: true,
      restApiName: 'Api for budgetBot',
      deploy: true,
      deployOptions: {
        stageName: 'prod',
        accessLogDestination: new apigateway.LogGroupLogDestination(apiAccessLogs),
        accessLogFormat: apigateway.AccessLogFormat.jsonWithStandardFields(),
      },
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: apigateway.Cors.ALL_METHODS,
      },
      policy: PolicyDocument.fromJson({
        Version: '2012-10-17',
        Statement: [{
          Effect: 'Deny',
          Principal: '*',
          Action: 'execute-api:*',
          Resource: `arn:aws:execute-api:${this.region}:${this.account}:*/*/*`,
          Condition: {
            NotIpAddress: {
              'aws:SourceIp': TELEGRAM_CIDR
            }
          }
        },
        {
          Effect: 'Allow',
          Principal: '*',
          Action: 'execute-api:Invoke',
          Resource: `arn:aws:execute-api:${this.region}:${this.account}:*/*/*`,
          Condition: {
              IpAddress: {
                'aws:SourceIp': TELEGRAM_CIDR
              }
          }
      },
      {
          Effect: 'Allow',
          Principal: '*',
          Action: 'execute-api:Invoke',
          Resource: `arn:aws:execute-api:${this.region}:${this.account}:*/*/*`
      }]})
    });

    const botEndpoint = api.root.addResource('budgetBot');

    botEndpoint.addMethod('POST', new apigateway.LambdaIntegration(botLambda), {
      authorizationType: apigateway.AuthorizationType.NONE
    });

    const bucket = new cdk.aws_s3.Bucket(this, 'BotCloudTrailBucket', {
      blockPublicAccess: cdk.aws_s3.BlockPublicAccess.BLOCK_ALL,
      encryption: cdk.aws_s3.BucketEncryption.S3_MANAGED,
      lifecycleRules: [{
        id: 'ExpireAfterOneDay',
        enabled: true,
        expiration: cdk.Duration.days(1)
      }]
    });

    const trail = new cloudtrail.Trail(this, 'BotLambdaTrail', {
      bucket,
      includeGlobalServiceEvents: false,
      isMultiRegionTrail: false,
      sendToCloudWatchLogs: true,
      cloudWatchLogsRetention: cdk.aws_logs.RetentionDays.ONE_DAY
    });

    trail.addEventSelector(cloudtrail.DataResourceType.LAMBDA_FUNCTION,
       [`arn:aws:lambda:${this.region}:${this.account}:function:${botLambda.functionName}`]
    );

    new CfnBudget(this, 'BudgetBotBudget', {
      budget: {
        budgetName: 'BudgetBot',
        budgetType: 'COST',
        timeUnit: 'MONTHLY',
        budgetLimit: {
          amount: 5,
          unit: 'USD'
        }
      },
      notificationsWithSubscribers: [
        {
          notification: {
            comparisonOperator: 'GREATER_THAN',
            notificationType: 'ACTUAL',
            threshold: 100
          },
          subscribers: [
            {
              address: process.env.ADMIN_EMAIL ?? "N/A",
              subscriptionType: 'EMAIL'
            }
          ]
        }
      ]
    });

    // Output the API endpoint
    new cdk.CfnOutput(this, 'API Endpoint', {
      value: api.url,
    });
  }
}
