package com.tenode.baleen.extras.jobs;

import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;

import com.tenode.baleen.extras.jobs.io.CsvInteractionReader;
import com.tenode.baleen.extras.jobs.io.CsvInteractionWriter;
import com.tenode.baleen.extras.jobs.io.MongoInteractionWriter;

import uk.gov.dstl.baleen.annotators.gazetteer.Mongo;
import uk.gov.dstl.baleen.resources.SharedMongoResource;
import uk.gov.dstl.baleen.uima.jobs.BaleenTask;
import uk.gov.dstl.baleen.uima.jobs.JobSettings;

/**
 * Upload interaction data from CSV to Mongo.
 *
 * The CSV will be in {@link CsvInteractionWriter} format and the Mongo output will be a
 * {@link Mongo} format.
 *
 * The upload job will typically look like:
 *
 * <pre>
 * mongo:
 *   db: baleen
 *   host: localhost
 *
 * job:
 *   tasks:
 *   - class: com.tenode.baleen.extras.jobs.UploadInteractionsToMongo
 *     input: output/interactions-enhanced.csv
 * </pre>
 *
 * The CSV file could be generated by a previous set in the process (ie through the
 * {@link IdentifyInteractions} task, or it should be generated or manually created previously. The
 * former provides a fast route to getting started, through typically the CSV will need some editing
 * to tune performance.
 *
 * Typically the extraction pipeline will then use the MongoStemming gazetteer:
 *
 * <pre>
 * - class: gazetteer.MongoStemming
 *   collection: interactions
 *   type: Interaction
 *
 * </pre>
 *
 * A second, optional, annotator can be used to filter relations to only the correct UIMA types.
 * This should be put after relationship extraction and will read from the database.
 *
 * <pre>
 * - class: com.tenode.baleen.extras.annotators.relationships.RelationTypeFilter
 * </pre>
 *
 * @baleen.javadoc
 *
 */
public class UploadInteractionsToMongo extends BaleenTask {
	/**
	 * Connection to Mongo
	 *
	 * @baleen.resource uk.gov.dstl.baleen.resources.SharedMongoResource
	 */
	public static final String KEY_MONGO = "mongo";
	@ExternalResource(key = KEY_MONGO)
	private SharedMongoResource mongo;

	/**
	 * Clear existing Mongo collections before uploading the new data.
	 *
	 * @baleen.config clear true
	 */
	public static final String KEY_CLEAR = "clear";
	@ConfigurationParameter(name = KEY_CLEAR, defaultValue = "true")
	private Boolean clearCollection;

	/**
	 * The name of the Mongo collection to outputs type (source, target, type) constraints too
	 *
	 * @baleen.config patterns relationTypes
	 */
	public static final String KEY_RELATIONSHIP_COLLECTION = "relationTypesCollection";
	@ConfigurationParameter(name = KEY_RELATIONSHIP_COLLECTION, defaultValue = "relationTypes")
	private String relationTypesCollection;

	/**
	 * The name of the Mongo collection to output the interaction words to (as a gazetteer)
	 *
	 * @baleen.config patterns interactions
	 */
	public static final String KEY_INTERACTION_COLLECTION = "interactionCollection";
	@ConfigurationParameter(name = KEY_INTERACTION_COLLECTION, defaultValue = "interactions")
	private String interactionCollection;

	/**
	 * The CSV file to load (written by CSVInteractionWriter)
	 *
	 * @baleen.config input interactions.csv
	 */
	public static final String KEY_CSV_FILENAME = "input";
	@ConfigurationParameter(name = KEY_CSV_FILENAME, defaultValue = "interactions.csv")
	private String inputFilename;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * uk.gov.dstl.baleen.uima.jobs.BaleenTask#execute(uk.gov.dstl.baleen.uima.jobs.JobSettings)
	 */
	@Override
	protected void execute(JobSettings settings) throws AnalysisEngineProcessException {

		try (MongoInteractionWriter writer = new MongoInteractionWriter(mongo.getDB(), relationTypesCollection,
				interactionCollection)) {

			if (clearCollection) {
				getMonitor().info("Clearing previous interacton collection");
				writer.clear();
			}

			final CsvInteractionReader reader = new CsvInteractionReader(inputFilename);
			reader.read((i, a) -> writer.write(i, a));
		} catch (final IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
		getMonitor().info("Finished uploading interactions to Mongo");
	}

}
