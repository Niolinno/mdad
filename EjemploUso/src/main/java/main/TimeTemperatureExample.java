package main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

import util.Cartesian;
import util.Pair;

/**
 * 
 * @author Dave
 *
 */
public class TimeTemperatureExample {

	/**
	 * Client for accessing DynamoDB
	 */
	static AmazonDynamoDBClient dynamoDB;

	/**
	 * Format and parse dates as HH:mm:ss such as 14:22:01
	 */
	static SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm:ss");

	/**
	 * Format and parse dates as dd-MM-YYYY such as 23-03-2017 (23d March)
	 */
	static SimpleDateFormat dayFormatter = new SimpleDateFormat("dd-MM-YYYY");

	/**
	 * Format and parse dates as dd such as 23, given only the day
	 */
	static SimpleDateFormat specificDayFormatter = new SimpleDateFormat("dd");

	/**
	 * Format and parse dates as MM such as 05, given only the month
	 */
	static SimpleDateFormat specificMonthFormatter = new SimpleDateFormat("MM");

	/**
	 * Format and parse dates as YYYY such as 2017, given only the year
	 */
	static SimpleDateFormat specificYearFormatter = new SimpleDateFormat("YYYY");

	
	static String TemperatureTableName = "Tiempo-Temperatura";

	/**
	 * The only information needed to create a client are security credentials
	 * consisting of the AWS Access Key ID and Secret Access Key. All other
	 * configuration, such as the service endpoints, are performed
	 * automatically. Client parameters, such as proxies, can be specified in an
	 * optional ClientConfiguration object when constructing a client.
	 *
	 * @see com.amazonaws.auth.BasicAWSCredentials
	 * @see com.amazonaws.auth.ProfilesConfigFile
	 * @see com.amazonaws.ClientConfiguration
	 */
	private static void init() throws Exception {
		/*
		 * The ProfileCredentialsProvider will return your [default] credential
		 * profile by reading from the credentials file located at
		 * (C:\\Users\\Dave\\.aws\\credentials).
		 */
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (C:\\Users\\Dave\\.aws\\credentials), and is in valid format.", e);
		}
		dynamoDB = new AmazonDynamoDBClient(credentials);
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		dynamoDB.setRegion(usWest2);
	}

	public static void main(String[] args) throws Exception {

		init();

		try {

			/*
			 * Comment all of this if table already exists and has its values
			 * and only wanna test the graph
			 */
			DeleteTableRequest deleteTableRequest = new DeleteTableRequest(TemperatureTableName);
			if (TableUtils.deleteTableIfExists(dynamoDB, deleteTableRequest)) {
				System.out.println("Deleting TABLE with name: " + TemperatureTableName);
			}

			createTable(TemperatureTableName, 10L, 5L, "Id", "N");
			loadContent("SalidaSensor.txt", TemperatureTableName);
			/*------------------------------------------------*/

			Cartesian.createPlane(average(getInfo(TemperatureTableName)));
			System.out.println("Success.");

		} catch (Exception e) {
			System.err.println("Program failed:");
			System.err.println(e.getMessage());
		}
	}

	private static void createTable(String tableName, long readCapacityUnits, long writeCapacityUnits,
			String hashKeyName, String hashKeyType) {

		createTable(tableName, readCapacityUnits, writeCapacityUnits, hashKeyName, hashKeyType, null, null);
	}

	private static void createTable(String tableName, long readCapacityUnits, long writeCapacityUnits,
			String hashKeyName, String hashKeyType, String rangeKeyName, String rangeKeyType) {

		try {

			// Create a table with a primary hash key named 'name', which holds
			// a string

			System.out.println("Creating TABLE with name: " + tableName);
			CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
					.withKeySchema(new KeySchemaElement().withAttributeName(hashKeyName).withKeyType(KeyType.HASH))
					.withAttributeDefinitions(new AttributeDefinition().withAttributeName(hashKeyName)
							.withAttributeType(ScalarAttributeType.N))
					.withProvisionedThroughput(
							new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

			// Create table if it does not exist yet
			System.out.println("Created TABLE with name: " + tableName);
			TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
			// wait for the table to move into ACTIVE state
			TableUtils.waitUntilActive(dynamoDB, tableName);
			System.out.println("Now the TABLE with name: " + tableName + " is ACTIVED");

			// Describe our new table
			DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
			TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
			System.out.println("Table Description: " + tableDescription);

		} catch (Exception e) {
			System.err.println("CreateTable request failed for " + tableName);
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Reads a file named as "fileName" given as a param and splits the content
	 * for each row into two pieces, first for timestamp and the second one for
	 * the temperature. For each row, calls
	 * {@link #loadData(String, String, String, int) loadData}, inserting the
	 * info into the DynamoDB table created at the beginning.
	 * 
	 * @param fileName
	 *            -> name of the file to be read and processed
	 * @param tableName
	 *            -> table name in which is going to be inserted each row of the
	 *            file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void loadContent(String fileName, String tableName) throws FileNotFoundException, IOException {
		String cadena;
		FileReader f = new FileReader(fileName);
		BufferedReader b = new BufferedReader(f);
		int i = 0;
		while ((cadena = b.readLine()) != null) {
			String[] parts = cadena.split("\\s+");
			for (String S : parts) {
				System.out.println(S);
				loadData(tableName, parts[0], parts[1], i);
			}
			System.out.println(" - ");
			i++;
		}
		b.close();
	}

	/**
	 * Loads a row into a specified table.
	 * 
	 * @param tableName
	 *            -> name of the table to be fill with the info in (works with a
	 *            time-temperature table based)
	 * @param timestamp
	 *            -> Represents the timestamp given to be loaded along its
	 *            temperature value
	 * @param temperatureValue
	 *            -> Represents the temperature value at a given timestamp
	 * @param i
	 *            -> generated id for primary key to be loaded for the resulted
	 *            item
	 */
	private static void loadData(String tableName, String timestamp, String temperatureValue, int i) {

		String temp_id = "Id", temp_time = "timestamp", temp_day = "Día", temp_hour = "Hora",
				temperature = "Temperatura";

		PutItemRequest putItemRequest;
		PutItemResult putItemResult;

		try {

			System.out.println("Adding data to " + tableName);

			Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
			item.put(temp_id, new AttributeValue().withN(Integer.toString(i)));
			item.put(temp_time, new AttributeValue().withS(timestamp));
			Date date = new Date(Long.parseLong(timestamp) * 1000L);
			item.put(temp_day, new AttributeValue().withS(dayFormatter.format(date).toString()));
			item.put(temp_hour, new AttributeValue().withS(hourFormatter.format(date).toString()));
			item.put(temperature, new AttributeValue().withS(temperatureValue));

			putItemRequest = new PutItemRequest(tableName, item);
			putItemResult = dynamoDB.putItem(putItemRequest);
			System.out.println("Result: " + putItemResult.toString());

		} catch (Exception e) {
			System.err.println("Failed to create item in " + tableName);
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Scan a table based on a given name but it's planned to work on a
	 * time-temperature based table. This method tries to find item names on the
	 * table named "timestamp" and "Temperature"
	 * 
	 * @param tableName
	 * @return List(Pair(String, String))
	 */
	private static List<Pair<String, String>> getInfo(String tableName) {

		ScanRequest scanRequest = new ScanRequest().withTableName(tableName);

		ScanResult result = dynamoDB.scan(scanRequest);
		List<Pair<String, String>> listaDatos = new ArrayList<Pair<String, String>>();
		for (Map<String, AttributeValue> item : result.getItems()) {
			listaDatos.add(new Pair<String, String>(item.get("timestamp").getS(), item.get("Temperatura").getS()));
		}
		return listaDatos;
	}

	/**
	 * Calculate average temperature per hour in a given List of Pairs,
	 * representing multiple time-temperature
	 * 
	 * @param list
	 * @return List(Pair(String, String))
	 */
	private static List<Pair<String, String>> average(List<Pair<String, String>> list) {

		List<Pair<String, String>> averageList = new ArrayList<Pair<String, String>>();

		int i = 0;
		while (!list.isEmpty()) {
			String timestamp = list.get(i).getFirst();

			Date date = new Date(Long.parseLong(timestamp) * 1000L);

			String[] hour = hourFormatter.format(date).toString().split("\\:");
			String day = specificDayFormatter.format(date).toString();
			String month = specificMonthFormatter.format(date).toString();
			String year = specificYearFormatter.format(date).toString();

			float sumTemperature = 0;
			float averCounter = 0;

			// System.out.println("***");

			int j = list.size() - 1;
			while (j >= 0) {

				Date date2 = new Date(Long.parseLong(list.get(j).getFirst()) * 1000L);
				String day2 = specificDayFormatter.format(date2).toString();
				String month2 = specificMonthFormatter.format(date2).toString();
				String year2 = specificYearFormatter.format(date2).toString();
				String[] hour2 = hourFormatter.format(date2).toString().split("\\:");

				// System.out.println(day2 + "/" + month2 + ", " + hour2[0] + "
				// -- " + list.get(j).getSecond());
				if (hour[0].equals(hour2[0]) && day2.equals(day) && month2.equals(month) && year2.equals(year)) {
					sumTemperature += Float.parseFloat(list.get(j).getSecond());
					averCounter += 1.0;
					list.remove(j);
				}

				j--;
			}
			// System.out.println("***");

			Float total = null;
			if (averCounter != 0.0)
				total = sumTemperature / averCounter;

			if (total != null)
				averageList.add(new Pair<String, String>(timestamp, total.toString()));

		}

		return averageList;

	}

}
