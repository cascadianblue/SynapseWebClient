package org.sagebionetworks.web.client.widget.entity.renderer;

import static org.sagebionetworks.web.client.ClientProperties.PLOTLY_JS;
import static org.sagebionetworks.web.shared.WidgetConstants.BAR_MODE;
import static org.sagebionetworks.web.shared.WidgetConstants.FILL_COLUMN_NAME;
import static org.sagebionetworks.web.shared.WidgetConstants.TABLE_QUERY_KEY;
import static org.sagebionetworks.web.shared.WidgetConstants.TITLE;
import static org.sagebionetworks.web.shared.WidgetConstants.TYPE;
import static org.sagebionetworks.web.shared.WidgetConstants.X_AXIS_TITLE;
import static org.sagebionetworks.web.shared.WidgetConstants.Y_AXIS_TITLE;
import static org.sagebionetworks.web.client.widget.entity.tabs.TablesTab.*;
import static org.sagebionetworks.web.client.place.Synapse.EntityArea.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.web.client.ArrayUtils;

import org.sagebionetworks.web.client.plotly.BarMode;
import org.sagebionetworks.web.client.plotly.GraphType;
import org.sagebionetworks.web.client.plotly.PlotlyTrace;
import org.sagebionetworks.web.client.plotly.PlotlyTraceWrapper;
import org.sagebionetworks.web.client.resources.ResourceLoader;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.widget.WidgetRendererPresenter;
import org.sagebionetworks.web.client.widget.asynch.AsynchronousJobTracker;
import org.sagebionetworks.web.client.widget.asynch.AsynchronousProgressWidget;
import org.sagebionetworks.web.client.widget.asynch.UpdatingAsynchProgressHandler;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;

import org.sagebionetworks.web.client.widget.table.v2.QueryTokenProvider;
import org.sagebionetworks.web.client.widget.table.v2.results.QueryBundleUtils;
import org.sagebionetworks.web.shared.WikiPageKey;
import org.sagebionetworks.web.shared.asynch.AsynchType;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * Example markdown:
 * ${graph?query=select animal%2C %27SF Zoo%27%2C %27LA Zoo%27 from syn8223164&type=SCATTER&title=plot test&xtitle=Animal&ytitle=Count}
 * @author Jay
 *
 */
public class PlotlyWidget implements PlotlyWidgetView.Presenter, WidgetRendererPresenter {
	
	private PlotlyWidgetView view;
	private Map<String, String> descriptor;
	private SynapseAlert synAlert;
	private String sql, title, xTitle, yTitle;
	GraphType graphType;
	BarMode barMode;
	private AsynchronousJobTracker jobTracker;
	// Mask to get all parts of a query.
	public static final Long ALL_PARTS_MASK = new Long(255);
	Query query;
	Long currentOffset;
	QueryBundleRequest qbr;
	public static final Long LIMIT = 150L;
	Map<String, List<String>> graphData;
	String xAxisColumnName, fillColumnName;
	private ResourceLoader resourceLoader;
	QueryTokenProvider queryTokenProvider;
	
	@Inject
	public PlotlyWidget(PlotlyWidgetView view,
			SynapseAlert synAlert,
			AsynchronousJobTracker jobTracker,
			ResourceLoader resourceLoader,
			QueryTokenProvider queryTokenProvider) {
		this.view = view;
		this.synAlert = synAlert;
		this.jobTracker = jobTracker;
		this.resourceLoader = resourceLoader;
		this.queryTokenProvider = queryTokenProvider;
		view.setSynAlertWidget(synAlert);
		view.setPresenter(this);
	}
	
	public void clear() {
		synAlert.clear();
		view.clearChart();
	}
	
	@Override
	public void configure(WikiPageKey wikiKey, Map<String, String> widgetDescriptor, Callback widgetRefreshRequired, Long wikiVersionInView) {
		//set up view based on descriptor parameters
		descriptor = widgetDescriptor;
		clear();
		graphData = null;
		xAxisColumnName = null;
		fillColumnName = descriptor.get(FILL_COLUMN_NAME);
		sql = descriptor.get(TABLE_QUERY_KEY);
		title = descriptor.get(TITLE);
		xTitle = descriptor.get(X_AXIS_TITLE);
		yTitle = descriptor.get(Y_AXIS_TITLE);
		graphType = GraphType.valueOf(descriptor.get(TYPE));
		
		if (descriptor.containsKey(BAR_MODE)) {
			barMode = BarMode.valueOf(descriptor.get(BAR_MODE));
		} else {
			barMode = BarMode.GROUP;
		}
		
		//gather the data, and then show the chart
		view.setLoadingVisible(true);
		view.setLoadingMessage("Loading...");
		currentOffset = 0L;
		query = new Query();
		query.setSql(sql);
		query.setIsConsistent(false);
		query.setLimit(LIMIT);
		
		qbr = new QueryBundleRequest();
		qbr.setPartMask(ALL_PARTS_MASK);
		qbr.setQuery(query);
		qbr.setEntityId(QueryBundleUtils.getTableId(query));
		
		String queryToken = queryTokenProvider.queryToToken(query);
		view.setSourceDataLinkVisible(false);
		view.setSourceDataLink(
				"#!Synapse:" + 
				qbr.getEntityId() + "/" + 
				TABLES.toString().toLowerCase() + "/" + 
				TABLE_QUERY_PREFIX + 
				queryToken);
		
		getMoreResults();
	}
	
	public void getMoreResults() {
		if (currentOffset > 0) {
			view.setLoadingMessage("Loaded " + currentOffset+" rows. ");	
		}
		// run the job
		query.setOffset(currentOffset);
		jobTracker.startAndTrack(AsynchType.TableQuery, qbr, AsynchronousProgressWidget.WAIT_MS, new UpdatingAsynchProgressHandler() {
			@Override
			public void onFailure(Throwable failure) {
				onCancel();
				synAlert.handleException(failure);
			}

			@Override
			public void onCancel() {
				view.clearChart();
				view.setLoadingVisible(false);
			}

			@Override
			public void onUpdate(AsynchronousJobStatus status) {
			}
			
			@Override
			public void onComplete(AsynchronousResponseBody response) {
				QueryResultBundle result = (QueryResultBundle) response;
				if (graphData == null) {
					initializeGraphData(result);
				}
				List<Row> rows = result.getQueryResult().getQueryResults().getRows();
				addRowData(result.getSelectColumns(), rows);
				currentOffset += LIMIT;
				if (rows.size() > 0 && currentOffset < result.getQueryCount()) {
					//get more results
					getMoreResults();
				} else {
					//we're done! send all of the results to the graph
					showChart();
				}
			}

			@Override
			public boolean isAttached() {
				return view.isAttached();
			}
		});
	}
	
	public void initializeGraphData(QueryResultBundle result) {
		graphData = new HashMap<String, List<String>>();
		if (result == null || result.getSelectColumns() == null || result.getSelectColumns().size() < 2) {
			return;
		}
		
		xAxisColumnName = result.getSelectColumns().get(0).getName();
		for (SelectColumn column : result.getSelectColumns()) {
			graphData.put(column.getName(), new ArrayList<String>());
		}
	}
	
	public void addRowData(List<SelectColumn> columns, List<Row> rows) {
		for (int i = 0; i < columns.size(); i++) {
			SelectColumn column = columns.get(i);
			List<String> colData = graphData.get(column.getName());
			for (Row row : rows) {
				colData.add(row.getValues().get(i));
			}
		}
	}
	
	public void showChart() {
		AsyncCallback<Void> initializedCallback = new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				showChart();
			}
			@Override
			public void onFailure(Throwable caught) {
				synAlert.handleException(caught);
			}
		};
		
		if (!resourceLoader.isLoaded(PLOTLY_JS)) {
			resourceLoader.requires(PLOTLY_JS, initializedCallback);
			return;
		}
		
		try {
			List<PlotlyTraceWrapper> plotlyGraphData;
			if (fillColumnName != null) {
				plotlyGraphData = transform(xAxisColumnName, fillColumnName, graphType, graphData);
			} else { 
				plotlyGraphData = transform(xAxisColumnName, graphType, graphData); 
			}
			view.setLoadingVisible(false);
			view.showChart(title, xTitle, yTitle, plotlyGraphData, barMode.toString().toLowerCase());
		} catch (Throwable ex) {
			synAlert.showError("Error showing plot: " + ex.getMessage());
		}
		view.setSourceDataLinkVisible(true);
	}
	
	
	/**
	 * Transforms graph data into standard x/y traces, used by xy line and bar charts.
	 * @param xAxisColumnName
	 * @param graphType
	 * @param graphData
	 * @return
	 */
	public static List<PlotlyTraceWrapper> transform(String xAxisColumnName, GraphType graphType, Map<String, List<String>> graphData) {
		String[] xData = ArrayUtils.getStringArray(graphData.remove(xAxisColumnName));
		List<PlotlyTraceWrapper> plotlyGraphData = new ArrayList<PlotlyTraceWrapper>(graphData.size());
		for (String columnName : graphData.keySet()) {
			PlotlyTraceWrapper trace =new PlotlyTraceWrapper();
			trace.setX(xData);
			String[] yData = ArrayUtils.getStringArray(graphData.get(columnName));
			trace.setY(yData);
			trace.setType(graphType);
			trace.setName(columnName);
			plotlyGraphData.add(trace);
		}
		return plotlyGraphData;
	}
	
	public static List<PlotlyTraceWrapper> transform(String xAxisColumnName, String fillColumnName, GraphType graphType, Map<String, List<String>> graphData) {
		String[] xData = ArrayUtils.getStringArray(graphData.remove(xAxisColumnName));
		String[] fillColumnData = ArrayUtils.getStringArray(graphData.remove(fillColumnName));
		if (xAxisColumnName.equals(fillColumnName)) {
			fillColumnData = xData;
		}
		
		Set<String> uniqueFillColumnDataValues = new HashSet<>();
		//get the unique values of the fill column
		for (String fillColValue : fillColumnData) {
			uniqueFillColumnDataValues.add(fillColValue);
		}
		List<PlotlyTraceWrapper> plotlyTraceData = new ArrayList<>();
		int yColumnCount = graphData.keySet().size();
		for (String columnName : graphData.keySet()) {
			for (String targetFillColumnValue : uniqueFillColumnDataValues) {
				// create a new trace for each fill column value
				PlotlyTraceWrapper newTrace = new PlotlyTraceWrapper();
				List<String> traceX = new ArrayList<>();
				List<String> traceY = new ArrayList<>();
				
				List<String> yData = graphData.get(columnName);
				for (int j = 0; j < fillColumnData.length; j++) {
					if (Objects.equals(targetFillColumnValue, fillColumnData[j])) {
						traceX.add(xData[j]);
						traceY.add(yData.get(j));
					}
				}
				
				newTrace.setX(ArrayUtils.getStringArray(traceX));
				newTrace.setY(ArrayUtils.getStringArray(traceY));
				newTrace.setType(graphType);
				String traceName = targetFillColumnValue == null ? "" : targetFillColumnValue;
				if (yColumnCount > 1) {
					traceName = columnName + " : " + traceName;
				}
				newTrace.setName(traceName);
				plotlyTraceData.add(newTrace);
			}
		}
		
		return plotlyTraceData;
	}
	
	@Override
	public Widget asWidget() {
		return view.asWidget();
	}
}
