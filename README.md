# Not finished yet !

docker run -p 9093:8080  -d --name keycloak18 jboss/keycloak:latest

docker update --restart=always keycloak18

/opt/jboss/keycloak/bin/add-user-keycloak.sh --user admin --password admin

docker restart keycloak18

docker cp com.ayoubanbara.keycloak-spi.jar [container_id]:/opt/jboss/keycloak/standalone/deployments

docker restart keycloak18
