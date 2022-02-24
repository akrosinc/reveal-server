package com.revealprecision.revealserver.util;

import com.revealprecision.revealserver.persistence.domain.actioncondition.Condition;
import com.revealprecision.revealserver.persistence.domain.actioncondition.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;


/**
 * Processes a Reveal frontend query string to build a Query Object
 *
 * Notation:
 *     query = {Person|Location}.[{tag|core}_{integer|date|string|boolean}]SPACE{"=" | "<" | ">" | "<=" | ">=" | "BETWEEN"}SPACE{value}
 *
 *
 *    Above SPACE refers to a real space.
 *    Above "{" characters and "}" is used for grouping in the notation representation, i.e. it must not be present in the query
 *
 * Rules for queryStr
 * 1. Only the following operators are allowed
 *      "=", ">", "<", ">=", "<=", "BETWEEN"
 * 2. All terms must space separated i.e.  Person.[tag_string]eye_color="brown" is not allowed since there must be spaces between "eye_color" and "=" or "=" and "brown"
 * 3. Only the following data types are allowed
 *     "integer", "string", "boolean", "date"
 * 4. Only the following field types are allowed
 *     "core", "tag"
 * 5. Where the operator "BETWEEN" is used, it must be preceded with a comma seperated pair
 *      example -> 10,20 for between 10 and 20
 *      or example -> 2012-10-01,2012-11-01 for between 2012-10-01 and 2012-11-01
 * 6. Only date and integer data types are supported for between
 * 7. Between includes the highest and lowest values specified in the search i.e. >= lowvalue and <= highvalue
 * 8. OR conditions can only be within parenthesis and reference the same field -> Person.[tag_string]eye_color="brown" OR Person.[tag_integer]weight=10 is NOT allowed
 *     correct use: ( Person.[tag_string]eye_color="brown" OR Person.[tag_string]eye_color="brown" ) AND Person.[tag_integer]weight=10
 * 9. Nested OR conditions are not supported
 * 10. AND conditions must ALWAYS be outside of parenthesis
 */
@Slf4j
public class ConditionQueryUtil {

  /**
   * Returns a Query object given a Reveal frontend query and the entity type
   * <p>
   * Given a Reveal frontend query Example:
   *
   * ( Person.[tag_string]eye_color = "brown" OR Person.[tag_string]eye_color = "black" ) AND Person.[tag_integer]weight>10
   *
   * It will return -
   * Query(
   *    orConditionList = {
   *         List.of [
   *                Condition(entity=Person, type=tag, dataType=string, operator="==", property=eye_color, value="brown",group=#group1),
   *                Condition(entity=Person, type=tag, dataType=string, operator="==", property=eye_color, value="black",group=#group1 )]
   *    },
   *    andConditionList = {
   *        List.of [
   *                Condition(entity=Person, type=tag, dataType=integer, operator=">", property=weight, value=10 ,group=null)
   *    },
   *    entity = Person
   */
  public static Query getQueryObject(String queryStr, String entityType) {

    List<Condition> andConditionList = new ArrayList<>();
    List<Condition> orConditionList = new ArrayList<>();

    if (!queryStr.equals("")) {

      Map<String, List<String>> queryMap = getQueryMapObjectOfOrConditions(
          queryStr);  //Or Conditions are added to map with keys "group<some integer>"

      String queryStrWithoutBracketedOrConditions = extractAndRemoveOrConditionsWithinParenthesis(
          queryStr); //Or Conditions are replaced from the string with the "group" name

      List<String> andConditions = splitByAND(queryStrWithoutBracketedOrConditions);

      for (String andCondition : andConditions) {
        String criteria = andCondition.trim();
        if (criteria.startsWith(
            "#")) {                       //this implies that the list has encountered a grouped list of Or Conditions
          List<String> orConditions = queryMap.get(
              criteria); //the Or Condition List are looked up from the HashMap and collected as Condition Objects
          for (String orCondition : orConditions) {
            Condition query = getQuery(orCondition, criteria);
            orConditionList.add(query);
          }

        } else {
          Condition query = getQuery(andCondition, null);
          andConditionList.add(query);
        }
      }
    }
    Query query = new Query();
    query.setAndConditions(andConditionList);
    query.setOrConditions(orConditionList);
    query.setEntity(entityType);
    return query;
  }

  private static String extractAndRemoveOrConditionsWithinParenthesis(String queryStr) {

    int groupCount = 0;

    Pattern pattern = Pattern.compile(
        "\\([^()]*\\)"); // pattern check for conditions in parenthesis
    Matcher matcher = pattern.matcher(queryStr);

    while (matcher.find()) {

      String foundStr = matcher.group();
      String key = "#group" + groupCount;
      queryStr = queryStr.replace(foundStr, key);
      matcher = pattern.matcher(queryStr);
      groupCount++;
    }
    return queryStr;
  }

  private static Map<String, List<String>> getQueryMapObjectOfOrConditions(String queryStr) {

    Map<String, List<String>> queryMap = new HashMap<>();

    int groupCount = 0;

    Pattern pattern = Pattern.compile(
        "\\([^()]*\\)"); // pattern check for conditions in parenthesis
    Matcher matcher = pattern.matcher(queryStr);

    while (matcher.find()) {
      String foundStr = matcher.group();
      List<String> orStrings = splitByOR(foundStr);
      String key = "#group" + groupCount;
      queryMap.put(key, orStrings);
      queryStr = queryStr.replace(foundStr, key);
      matcher = pattern.matcher(queryStr);
      groupCount++;
    }
    return queryMap;
  }

  /**
   * Given a Reveal frontend condition the method will build Condition Object
   * <p>
   * Example: condition=Person.[tag_string]eye_color = "brown" group=#group0 returns
   * Condition(entity=Person, type=tag, dataType=string, operator="==",group=#group1)
   * <p>
   * Where the operator BETWEEN is used the method will expect a comma separated value representing
   * a range example value = 10,10
   */
  private static Condition getQuery(String condition, String group) {

    Pattern tagPattern = Pattern.compile(
        "\\[[^()]*\\]"); // Extract the field type eg. tag or core within and including brackets' [...]

    Condition conditionObj = new Condition();
    List<String> strings = Arrays.asList(condition.trim().split(" "));
    String entity = strings.get(0).split("\\.")[0]; // as per example above entity = person
    conditionObj.setEntity(entity);
    String property = strings.get(0).split("\\.")[1]; // as per example above property = birth_date
    Matcher tagMatcher = tagPattern.matcher(property);

    if (tagMatcher.find()) {
      String tagFound = tagMatcher.group();
      String cleanedTag = tagFound.replace("[", "").replace("]", "");
      List<String> typeAndDataType = Arrays.asList(cleanedTag.split("_"));

      conditionObj.setType(typeAndDataType.get(0)); // as per example above type = core
      conditionObj.setDataType(typeAndDataType.get(1)); // as per example above dataType = date
      String regex = "\\" + tagFound + "\\b";
      conditionObj.setProperty(property.replaceAll(regex, ""));
    }
    if (group != null) {
      conditionObj.setGroup(group);
    }

    for (String s : strings) {
      if (s.contains("==") || s.contains(">=") || s.contains("<=") || s.contains(">") || s.contains(
          "<") || s.contains("BETWEEN")) {
        conditionObj.setOperator(s);

        if (s.equals("BETWEEN")) {
          conditionObj.setValue(Arrays.asList(strings.get(strings.size() - 1)
              .split(","))); //get comma separated range value set for between cases
        } else {
          conditionObj.setValue(List.of(strings.get(strings.size() - 1)));
        }
      }
    }
    return conditionObj;
  }

  static List<String> splitByOR(String query) {

    query = query.replaceFirst("\\(", "");
    query = removeLast(query, ")");
    return Arrays.asList(query.split("OR"));

  }

  static List<String> splitByAND(String query) {

    query = query.replaceFirst("\\(", "");
    query = removeLast(query, ")");
    return Arrays.asList(query.split("AND"));

  }


  private static String removeLast(String s, String search) {
    int pos = s.lastIndexOf(search);
    if (pos > -1) {
      return s.substring(0, pos) + s.substring(pos + search.length());
    }
    return s;
  }

}


