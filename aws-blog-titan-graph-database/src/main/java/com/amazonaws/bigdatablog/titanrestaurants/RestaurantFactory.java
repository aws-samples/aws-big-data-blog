package com.amazonaws.bigdatablog.titanrestaurants;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;

import com.thinkaurelius.titan.core.Multiplicity;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanVertex;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import au.com.bytecode.opencsv.CSVReader;

public class RestaurantFactory {

	public static final String RESTAURANT_ID = "RestaurantId";
	public static final String USER_ID = "userId";
	public static final String GENRE = "genreId";

	public static final String PROP_RESTAURANT_NAME = "restaurant_name";
	public static final String PROP_RESTAURANT_LATITUDE = "restaurant_latitude";
	public static final String PROP_RESTAURANT_LONGITUDE = "restaurant_longitude";
	public static final String PROP_RESTAURANT_PLACE = "restaurant_place";
	public static final String PROP_RESTAURANT_ALCOHOL = "restaurant_alcohol";
	public static final String PROP_RESTAURANT_DRESSCODE = "restaurant_dresscode";
	public static final String PROP_RESTAURANT_ACCESSIBILITY = "restaurant_accessibility";
	public static final String PROP_RESTAURANT_PRICE = "restaurant_price";
	public static final String PROP_RESTAURANT_AMBIENCE = "restaurant_ambience";
	public static final String PROP_RESTAURANT_FRANCHISE = "restaurant_franchise";
	public static final String PROP_RESTAURANT_SERVICES = "restaurant_services";

	public static final String PROP_USER_DRESSPREFERENCE = "user_dresspreference";
	public static final String PROP_USER_BUDGET = "user_budget";

	public static final String PROP_VISIT_RATING = "visit_rating";
	public static final String PROP_VISIT_FOOD = "visit_food";
	public static final String PROP_VISIT_SERVICE = "visit_service";

	public static void load(final TitanGraph graph) throws Exception {

		// set up the structure of our graph
		TitanManagement mgmt = graph.getManagementSystem();

		if (mgmt.getGraphIndex(RESTAURANT_ID) == null) {
			final PropertyKey RESTAURANTIdKey = mgmt.makePropertyKey(RESTAURANT_ID).dataType(Integer.class).make();
			mgmt.buildIndex(RESTAURANT_ID, Vertex.class).addKey(RESTAURANTIdKey).unique().buildCompositeIndex();
			mgmt.makePropertyKey(PROP_RESTAURANT_NAME).dataType(String.class).make();
			mgmt.makePropertyKey(PROP_RESTAURANT_LATITUDE).dataType(Double.class).make();
			mgmt.makePropertyKey(PROP_RESTAURANT_LONGITUDE).dataType(Double.class).make();
			mgmt.makePropertyKey(PROP_RESTAURANT_PLACE).dataType(Geoshape.class).make();
			mgmt.makePropertyKey(PROP_RESTAURANT_ALCOHOL).dataType(String.class).make();
			mgmt.makePropertyKey(PROP_RESTAURANT_DRESSCODE).dataType(String.class).make();
			mgmt.makePropertyKey(PROP_RESTAURANT_ACCESSIBILITY).dataType(String.class).make();
			mgmt.makePropertyKey(PROP_RESTAURANT_PRICE).dataType(String.class).make();
			mgmt.makePropertyKey(PROP_RESTAURANT_AMBIENCE).dataType(String.class).make();
			mgmt.makePropertyKey(PROP_RESTAURANT_FRANCHISE).dataType(String.class).make();
			mgmt.makePropertyKey(PROP_RESTAURANT_SERVICES).dataType(String.class).make();
		}

		if (mgmt.getGraphIndex(USER_ID) == null) {
			final PropertyKey userKey = mgmt.makePropertyKey(USER_ID).dataType(String.class).make();
			mgmt.buildIndex(USER_ID, Vertex.class).addKey(userKey).unique().buildCompositeIndex();
			mgmt.makePropertyKey(PROP_USER_DRESSPREFERENCE).dataType(String.class).make();
			mgmt.makePropertyKey(PROP_USER_BUDGET).dataType(String.class).make();
		}

		if (mgmt.getGraphIndex(GENRE) == null) {
			final PropertyKey genreKey = mgmt.makePropertyKey(GENRE).dataType(String.class).make();
			mgmt.buildIndex(GENRE, Vertex.class).addKey(genreKey).unique().buildCompositeIndex();
		}

		mgmt.makeVertexLabel("restaurant").make();
		mgmt.makeVertexLabel("genre").make();
		mgmt.makeVertexLabel("user").make();

		mgmt.makeEdgeLabel("visit").multiplicity(Multiplicity.MULTI).make();
		mgmt.makePropertyKey(PROP_VISIT_RATING).dataType(Integer.class).make();
		mgmt.makePropertyKey(PROP_VISIT_FOOD).dataType(Integer.class).make();
		mgmt.makePropertyKey(PROP_VISIT_SERVICE).dataType(Integer.class).make();

		mgmt.makeEdgeLabel("restaurant_genre").multiplicity(Multiplicity.MULTI).make();

		mgmt.makeEdgeLabel("friend").multiplicity(Multiplicity.MULTI).make();

		mgmt.commit();

		// load the restaurants

		ClassLoader classLoader = RestaurantFactory.class.getClassLoader();
		URL resource = classLoader.getResource("restaurants.csv");

		try (CSVReader reader = new CSVReader(new InputStreamReader(resource.openStream()))) {
			String[] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				TitanVertex vertex = graph.addVertexWithLabel("restaurant");
				vertex.addProperty(RESTAURANT_ID, nextLine[0]);
				vertex.addProperty(PROP_RESTAURANT_NAME, nextLine[1]);
				vertex.addProperty(PROP_RESTAURANT_LATITUDE, nextLine[2]);
				vertex.addProperty(PROP_RESTAURANT_LONGITUDE, nextLine[3]);
				vertex.addProperty(PROP_RESTAURANT_ALCOHOL, nextLine[4]);
				vertex.addProperty(PROP_RESTAURANT_DRESSCODE, nextLine[5]);
				vertex.addProperty(PROP_RESTAURANT_ACCESSIBILITY, nextLine[6]);
				vertex.addProperty(PROP_RESTAURANT_PRICE, nextLine[7]);
				vertex.addProperty(PROP_RESTAURANT_AMBIENCE, nextLine[8]);
				vertex.addProperty(PROP_RESTAURANT_FRANCHISE, nextLine[9]);
				vertex.addProperty(PROP_RESTAURANT_SERVICES, nextLine[10]);

				if (nextLine[2] != null && nextLine[3] != null && !nextLine[2].isEmpty() && !nextLine[3].isEmpty()) {
					double latitude = Double.parseDouble(nextLine[2]);
					double longitude = Double.parseDouble(nextLine[3]);
					vertex.addProperty(PROP_RESTAURANT_PLACE, Geoshape.point(latitude, longitude));
				}

			}
		}

		// load the genres (types of cuisine)
		resource = classLoader.getResource("genres.csv");

		try (CSVReader reader = new CSVReader(new InputStreamReader(resource.openStream()))) {
			String[] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				TitanVertex vertex = graph.addVertexWithLabel("genre");
				vertex.addProperty(GENRE, nextLine[0]);

			}
		}

		// load the customers (users)
		resource = classLoader.getResource("users.csv");

		try (CSVReader reader = new CSVReader(new InputStreamReader(resource.openStream()))) {
			String[] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				TitanVertex vertex = graph.addVertexWithLabel("user");
				vertex.addProperty(USER_ID, nextLine[0]);
				vertex.addProperty(PROP_USER_DRESSPREFERENCE, nextLine[1]);
				vertex.addProperty(PROP_USER_BUDGET, nextLine[2]);

			}
		}

		// load some visits (from user to restaurant)
		resource = classLoader.getResource("visits.csv");

		try (CSVReader reader = new CSVReader(new InputStreamReader(resource.openStream()))) {
			String[] nextLine;
			while ((nextLine = reader.readNext()) != null) {

				Vertex userVertex = get(graph, USER_ID, nextLine[0]);
				Vertex RestaurantVertex = get(graph, RESTAURANT_ID, nextLine[1]);

				Edge visitEdge = userVertex.addEdge("visit", RestaurantVertex);

				visitEdge.setProperty(PROP_VISIT_RATING, nextLine[2]);
				visitEdge.setProperty(PROP_VISIT_FOOD, nextLine[3]);
				visitEdge.setProperty(PROP_VISIT_SERVICE, nextLine[4]);

			}
		}

		// load the genre for each restaurant
		resource = classLoader.getResource("restaurantgenres.csv");

		try (CSVReader reader = new CSVReader(new InputStreamReader(resource.openStream()))) {
			String[] nextLine;
			while ((nextLine = reader.readNext()) != null) {

				Vertex RESTAURANTVertex = get(graph, RESTAURANT_ID, nextLine[0]);
				Vertex genreVertex = get(graph, GENRE, nextLine[1]);

				if (RESTAURANTVertex != null && genreVertex != null) {
					RESTAURANTVertex.addEdge("restaurant_genre", genreVertex);
				}
			}
		}

		// load the friends
		resource = classLoader.getResource("friends.csv");

		try (CSVReader reader = new CSVReader(new InputStreamReader(resource.openStream()))) {
			String[] nextLine;
			while ((nextLine = reader.readNext()) != null) {

				Vertex userVertex = get(graph, USER_ID, nextLine[0]);
				Vertex friendVertex = get(graph, USER_ID, nextLine[2]);

				if (userVertex != null && friendVertex != null) {
					userVertex.addEdge("friend", friendVertex);
				}
			}
		}

	}

	// helper function to get a vertex based on a property
	private static Vertex get(TitanGraph graph, String key, String value) {
		Iterator<Vertex> it = graph.getVertices(key, value).iterator();
		Vertex vertex = null;
		if (it.hasNext()) {
			vertex = it.next();
		}
		return vertex;

	}
}
