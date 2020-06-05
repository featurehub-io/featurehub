package io.featurehub.db.utils;

/**
 * * by Richard Vowles (https://www.google.com/+RichardVowles)
 */
public class DbOpenTracing {
	public static String rewriteDatabaseUrl(String dbUrl) {
		if (dbUrl.startsWith("jdbc:")) {
			return "jdbc:p6spy:" + dbUrl.substring(5);
		} else {
			return dbUrl;
		}
	}
}
