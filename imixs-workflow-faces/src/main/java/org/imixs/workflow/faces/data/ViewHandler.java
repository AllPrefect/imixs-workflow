/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.faces.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The ViewHandler is a @RequestScoped CDI bean computing the result defined by
 * a ViewController.
 * 
 * @author rsoika
 * @version 0.0.1
 */
@Named
@RequestScoped
public class ViewHandler implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<Integer, List<ItemCollection>> data = null;

	private static Logger logger = Logger.getLogger(ViewHandler.class.getName());

	@EJB
	DocumentService documentService;

	public ViewHandler() {
		super();
		logger.info("...construct...");
	}

	@PostConstruct
	public void init() {
		logger.info("init...");
		data = new HashMap<Integer, List<ItemCollection>>();
	}

	/**
	 * This method can be used in ajax forms to pre-compute the result set for
	 * further rendering.
	 * 
	 * @param viewController
	 * @throws QueryException
	 */
	public void onLoad(ViewController viewController) throws QueryException {
		getData(viewController);
	}

	public void forward(ViewController viewController) {
		data.remove(getHashKey(viewController));
		viewController.setPageIndex(viewController.getPageIndex()+1);
	}

	public void back(ViewController viewController) {
		data.remove(getHashKey(viewController));
		int i=viewController.getPageIndex();
		i--;
		if (i < 0) {
			i = 0;
		}
		viewController.setPageIndex(i);
	}

	/**
	 * Returns the current view result. The returned result set is defined by the
	 * current query definition.
	 * <p>
	 * The method implements a lazy loading mechanism and caches the result locally.
	 * 
	 * @return view result
	 * @throws QueryException
	 */
	public List<ItemCollection> getData(ViewController viewController) throws QueryException {

		if (viewController == null) {
			return new ArrayList<ItemCollection>();
		}

		if (viewController.getQuery() == null || viewController.getQuery().isEmpty()) {
			// no query defined
			logger.warning("ViewController - now query defined!");
			return new ArrayList<ItemCollection>();
		}

		// Caching mechanism - verify if data is already cached
		List<ItemCollection> result = data.get(getHashKey(viewController));
		if (result != null) {
			// return a cached result set
			return result;
		}

		// here we compute the result only in the RENDER_RESPONSE phase
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {

			// load data
//			logger.info("...... get data - query=" + viewController.getQuery() + " pageIndex="
//					+ viewController.getPageIndex());
			result =viewController.loadData();
//			documentService.find(viewController.getQuery(), viewController.getPageSize(),
//					viewController.getPageIndex(), viewController.getSortBy(), viewController.isSortReverse());

			// The end of a list is reached when the size is below or equal the
			// pageSize. See issue #287
//			if (result.size() < viewController.getPageSize()) {
//				viewController.setEndOfList(true);
//			} else {
//				// look ahead if we have more entries...
//				int iAhead = (viewController.getPageSize() * (viewController.getPageIndex() + 1)) + 1;
//				if (documentService.count(viewController.getQuery(), iAhead) < iAhead) {
//					// there is no more data
//					viewController.setEndOfList(true);
//				} else {
//					viewController.setEndOfList(false);
//				}
//			}

			logger.info("...cache with hash=" + getHashKey(viewController));
			// cache result
			data.put(getHashKey(viewController), result);
		}

		return result;
	}

	private int getHashKey(ViewController viewController) {
		if (viewController == null) {
			return -1;
		}
		String h = viewController.getQuery() + viewController.getPageIndex() + viewController.getPageSize();
		return h.hashCode();
	}
}