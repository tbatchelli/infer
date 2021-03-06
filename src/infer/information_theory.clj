(ns infer.information-theory
  (:use [infer.probability :only [comb-merge]])
  (:use [infer.core :only [flatten-with log2]]))

;;TODO change hte lib's interface from maps of counts to vectors/matrices of counts.

(defn gini-impurity
  "Gini impurity is  measure of how often a chosen element from the set would be
   incorrectly labelled if it were randomly labelled according to the
   distribution of labels in the subset. Gini impurity can be computed by
   summing the probability of each item being chosen times the probability of a
   mistake in categorizing that item. It reaches its minimum (zero) when all
   cases in the node fall into a single target category."
  [counts]
  (let [total (apply + (vals counts))]
    (apply +
	   (for [[k v] counts
		 [k1 v1] counts
		 :when (not (= k1 k))]
	     (* (/ v total)
		(/ v1 total))))))

(defn kl-divergence
  "http://en.wikipedia.org/wiki/Kullback%E2%80%93Leibler_divergence

   In probability theory and information theory, the Kullback–Leibler divergence
   (also information divergence, information gain, or relative entropy) is a
   non-symmetric measure of the difference between two probability distributions
   P and Q. KL measures the expected number of extra bits required to code
   samples from P when using a code based on Q, rather than using a code based
   on P. Typically P represents the true distribution of data, observations, or
   a precise calculated theoretical distribution. The measure Q typically
   represents a theory, model, description, or approximation of P.

   Takes a map of class label to counts.
   Note the (> p 0) predicate defines (* 0 (log2 0)) as 0 rather than NaN.
   Assumes you pass in distributions as nested maps and flattens them before
   applying the algorithm."
  [p-counts q-counts]
  (let [p-counts (flatten-with str p-counts)
	q-counts (flatten-with str q-counts)
        p-total (apply + (vals p-counts))
	q-total (apply + (vals q-counts))]
    (apply +
	   (for [[k v] p-counts
		 :let [p (/ v p-total)
		       q (/ (q-counts k) q-total)]
		 :when (and (> p 0) (> q 0))]
	     (* p
		   (log2 (/ p q)))))))

;;TODO: remove deplication between entropy, gain, and kl divergences
(defn entropy
  "Takes a map of class label to counts."
  [counts]
  (let [total (apply + (vals counts))]
    (apply +
	    (for [[k v] counts
		    :let [p (/ v total)]
	            ;;defines (* 0 (log2 0)) as 0 rather than Nan
		    :when (> p 0)]
	     (- (* p
		    (log2 p)))))))

(defn gain
  "Computes information gain from count matrix of feature class labels to
   predicted class labels.

   => (gain {:weak {:positive 6 :negative 2}
		         :strong {:positive 3 :negative 3}})"
  [data]
  (let [totals (apply merge-with + (vals data))
	      total (apply + (vals totals))
	      total-entropy (entropy totals)]
    (- total-entropy
       (apply +
	  (for [[k v] data
		:let [attr-total (apply + (vals v))]
		:when (> attr-total 0)]
		  (* (/ attr-total total)
		     (entropy v)))))))

(defn total-correlation
  "Total correlation is a multivariate generalization of mutual information. It
   is the Kullback-Leibler divergence between the joint distribution of a set
   and its maximum entropy product approximation.
   
   See: http://en.wikipedia.org/wiki/Total_correlation"
  [joint individuals]
  ;;merge individualss for maximum entropy product approximation.
  ;;can merge here due to transitivity of the product of dividend.
  (kl-divergence joint (apply comb-merge * individuals)))

(defn mutual-information
  "Mutual information is the Kullback-Leibler divergence between the product of
   the marginal distributions of two random variables, p(x)p(y), and the the
   random variables' joint distribution, p(x,y).

   See: http://en.wikipedia.org/wiki/Mutual_information"
  [joint individuals]
  (total-correlation joint individuals))

; TODO: other divergences and distances that can be described as kl divergences
; http://en.wikipedia.org/wiki/Hellinger_distance
; http://en.wikipedia.org/wiki/Interaction_information
; http://en.wikipedia.org/wiki/Kirkwood_approximation
; http://en.wikipedia.org/wiki/F-divergence
; http://en.wikipedia.org/wiki/Statistical_distance
