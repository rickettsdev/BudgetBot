This is a AWS 'Serverless' Telegram bot. 

This assumes that you have workspace to run the AWS CLI, Java 11 at minimum with Gradle, and NPM configured.

First, head to packages/JavaLambda/. Here you will run gradle build. Then you will head to packages/CDK/

There are 3 .env variables required to configure this for use.

* BOT_TOKEN: You can retreive this from botfather when you create a Telegram bot.
* CHAT_ID:  You can get this from IDBot.
* ADMIN_EMAIL: Email you'd like your budget threshold monitor to activate.

After you have filled out your packages/CDK/.env file, run cdk deploy. Approve the changes and the bot should be deployed to your AWS account.

If you aren't frequently engaging the Telegram bot, it may take a few seconds to respond back with a message. This additional latency is Lambda Cold Start. 
