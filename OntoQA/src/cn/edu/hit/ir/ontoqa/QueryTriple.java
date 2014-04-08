/**
 * QueryTriple.java
 * edu.hit.ir.ontoqa
 * Function:	TODO
 * Reason:		TODO 
 *
 *   ver     date      		author
 * ──────────────────────────────────
 *  0.1.0	2011-2-28 		bin3
 *
 * Copyright (c) 2011, CIR@HIT All Rights Reserved.
*/

package cn.edu.hit.ir.ontoqa;

/**
 * ClassName:QueryTriple
 * Function: TODO
 *
 * @author   bin3
 * @version  
 * @date	 2011	2011-2-28		下午08:16:51
 */

public class QueryTriple {	
	public QueryTripleElement subject;
	public QueryTripleElement predicate;
	public QueryTripleElement object;
	
	/**
	 * Creates a new instance of QueryTriple.
	 *
	 */
	public QueryTriple(QueryTripleElement subject,
			QueryTripleElement predicate, QueryTripleElement object) {
		// TODO Auto-generated constructor stub
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}
	
	public String toString() {
		String string = "<" + subject + ", " + predicate + ", " + object + ">";
		return string;
	}
}
