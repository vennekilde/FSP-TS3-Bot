# To change this license header, choose License Headers in Project Properties.
# To change this template file, choose Tools | Templates
# and open the template in the editor.
FROM openjdk:8

ENV VERSION 4.0.4

ADD https://github.com/vennekilde/FSP-TS3-Bot/releases/download/${VERSION}/TeamspeakServerLink.jar /app/TeamspeakServerLink.jar

COPY ts3bot-entrypoint.sh /app/ts3bot-entrypoint.sh

RUN chmod +x /app/ts3bot-entrypoint.sh
WORKDIR /app
ENTRYPOINT ["/app/ts3bot-entrypoint.sh"]
