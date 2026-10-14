package com.archanchor.services.rating;

import com.archanchor.domains.rating.RatingWeights;
import org.springframework.stereotype.Component;

/**
 * The weights the election and the analyses use right now: those of the project whose
 * modules are saved. The server holds one analysed project at a time
 * ({@code modules.bin}), so there is one set of weights in force, replaced when another
 * project is analysed or when the architect changes them (#27).
 *
 * <pre>
 * activeWeights.adopt("bookstore", ratingWeightsStore.weightsOf("bookstore"));
 * double rate = activeWeights.current().recommendation().rate(similarityImprovement, violationsImprovement);
 * </pre>
 */
@Component
public class ActiveRatingWeights {

	// One field, so a reader never sees the weights of one project paired with the name
	// of another.
	private volatile InForce inForce;

	/** Equal weights, for no project in particular, until a project is analysed. */
	public ActiveRatingWeights() {
		this(RatingWeights.EQUAL);
	}

	public ActiveRatingWeights(RatingWeights weights) {
		this.inForce = new InForce(null, weights);
	}

	public RatingWeights current() {
		return inForce.weights();
	}

	public void adopt(String projectName, RatingWeights weights) {
		inForce = new InForce(projectName, weights);
	}

	/** Whether the saved modules, and so these weights, belong to that project. */
	public boolean appliesTo(String projectName) {
		return projectName != null && projectName.equals(inForce.projectName());
	}

	private record InForce(String projectName, RatingWeights weights) {
	}

}
