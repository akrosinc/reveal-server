package com.revealprecision.revealserver.util;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class SnakeCasePhysicalNamingStrategy extends PhysicalNamingStrategyStandardImpl {
    public static final String CAMEL_CASE_REGEX = "([a-z]+)([A-Z]+)";
    public static final String SNAKE_CASE_PATTERN = "$1\\_$2";

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment context) {
        return convertToSnakeCase(super.toPhysicalCatalogName(name, context));
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment context) {
        return convertToSnakeCase(super.toPhysicalSchemaName(name, context));
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        return convertToSnakeCase(super.toPhysicalTableName(name, context));
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment context) {
        return convertToSnakeCase(super.toPhysicalSequenceName(name, context));
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        return convertToSnakeCase(super.toPhysicalColumnName(name, context));
    }

    private Identifier convertToSnakeCase(Identifier identifier){
          if(identifier != null){
              String name = identifier.getText();
              String formattedName = name.replaceAll(CAMEL_CASE_REGEX,SNAKE_CASE_PATTERN).toLowerCase();
              return !formattedName.equals(name) ? Identifier.toIdentifier(formattedName, identifier.isQuoted()) : identifier;
          } else {
              return null;
          }
    }
}