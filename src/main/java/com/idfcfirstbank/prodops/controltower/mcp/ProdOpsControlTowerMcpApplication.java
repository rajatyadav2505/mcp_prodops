package com.idfcfirstbank.prodops.controltower.mcp;

import com.idfcfirstbank.prodops.controltower.mcp.config.ProdOpsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = ProdOpsProperties.class)
public class ProdOpsControlTowerMcpApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProdOpsControlTowerMcpApplication.class, args);
  }
}
