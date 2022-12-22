package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class AluraRdsStack extends Stack {
    public AluraRdsStack(final Construct scope, final String id, final Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public AluraRdsStack(final Construct scope, final String id, final StackProps props, final Vpc vpc) {
        super(scope, id, props);

        // CFN Parameter (parâmetro do CloudFormation).
        // Esse parâmetro vai permitir que, na hora do deploy, indiquemos qual a senha para acessar esse banco.
        CfnParameter senha = CfnParameter.Builder.create (this, "senha")
            .type("String")
            .description("Senha do database pedidos-ms")
            .build();

        // Security group da VPC padrão que foi criada.
        ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));

        // Database instance: criando a instância na AWS
        DatabaseInstance database = DatabaseInstance.Builder
                .create(this, "Rds-pedidos")
                .instanceIdentifier("alura-aws-pedido-db")
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                        .version(MysqlEngineVersion.VER_8_0)
                        .build()))
                .vpc(vpc)
                .credentials(Credentials.fromUsername("admin",
                CredentialsFromUsernameOptions.builder()
                        .password(SecretValue.unsafePlainText(senha.getValueAsString()))
                        .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(10)
                .securityGroups(Collections.singletonList(iSecurityGroup))
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getPrivateSubnets())
                        .build())
                .build();

        CfnOutput.Builder.create(this, "pedidos-db-endpoint")
                .exportName("pedidos-db-endpoint")
                .value(database.getDbInstanceEndpointAddress())
                .build();

        CfnOutput.Builder.create(this, "pedidos-db-senha")
                .exportName("pedidos-db-senha")
                .value(senha.getValueAsString())
                .build();
    }
}
