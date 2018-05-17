package org.compiere.model;

import java.awt.Color;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

public class MChart extends X_AD_Chart {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8851465915516536910L;
	
	private int windowNo=0;
	private Dataset dataset;
	private HashMap<String,MQuery> queries;

	private String m_newName;

	private String m_newRangeLabel;

	private String m_newDomainLabel;

	public MChart(Properties ctx, int AD_Chart_ID, String trxName) {
		super(ctx, AD_Chart_ID, trxName);		
	}

	public MChart(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public void loadData() {
		queries = new HashMap<String,MQuery>();
		for ( MChartDatasource ds : getDatasources() )
		{
			ds.addData(this);
		}
		
	}

	public CategoryDataset getCategoryDataset() {
		dataset = new DefaultCategoryDataset();
		loadData();
		return (CategoryDataset) dataset;
	}
	
	public IntervalXYDataset getXYDataset() {
		dataset = new TimeSeriesCollection();
		loadData();
		return (IntervalXYDataset) dataset;
	}

	public PieDataset getPieDataset() {
		dataset = new DefaultPieDataset();
		loadData();
		return (PieDataset) dataset;
	}
	
	public Dataset getDataset() {
		return dataset;
	}
	
	private List<MChartDatasource> getDatasources() {
		
		return new Query(getCtx(), MChartDatasource.Table_Name, MChart.COLUMNNAME_AD_Chart_ID + "=?", null)
		.setParameters(getAD_Chart_ID()).setOnlyActiveRecords(true).list();
	}
	
	public HashMap<String, MQuery> getQueries() {
		return queries;
	}

	public void setWindowNo(int windowNo) {
		this.windowNo = windowNo;		
	}
	
	public int getWindowNo() {
		return windowNo;
	}

	public MQuery getQuery(String key) {
		

		if ( queries.containsKey(key) )
		{
			return queries.get(key);
		}
		
		return null;
	}

	/**
	 *
	 * @param type
	 * @return JFreeChart
	 */
	public JFreeChart createChart() {
		
		String type = getChartType();
		
		if(MChart.CHARTTYPE_BarChart.equals(type))
		{
			if ( isTimeSeries())
			{
				return createXYBarChart();
			}
			return createBarChart();
		}
		else if (MChart.CHARTTYPE_3DBarChart.equals(type))
		{
			return create3DBarChart();
		}
		else if (MChart.CHARTTYPE_StackedBarChart.equals(type))
		{

			if ( isTimeSeries())
				return createXYBarChart();
			
			return createStackedBarChart();
		}
		else if (MChart.CHARTTYPE_3DStackedBarChart.equals(type))
		{
			return create3DStackedBarChart();
		}
		else if (MChart.CHARTTYPE_3DPieChart.equals(type))
		{
			return create3DPieChart();
		}
		else if (MChart.CHARTTYPE_PieChart.equals(type))
		{
			return createPieChart();
		}
		else if (MChart.CHARTTYPE_3DLineChart.equals(type))
		{
			return create3DLineChart();
		}
		else if (MChart.CHARTTYPE_AreaChart.equals(type))
		{
			return createAreaChart();
		}
		else if (MChart.CHARTTYPE_StackedAreaChart.equals(type))
		{
			return createStackedAreaChart();
		}
		else if (MChart.CHARTTYPE_LineChart.equals(type))
		{
			if ( isTimeSeries() )
				return createTimeSeriesChart();
			return createLineChart();
		}
		else if (MChart.CHARTTYPE_RingChart.equals(type))
		{
			return createRingChart();
		}
		else if (MChart.CHARTTYPE_WaterfallChart.equals(type))
		{
			return createWaterfallChart();
		}
		else
		{
			throw new IllegalArgumentException("unknown chart type=" + type);
		}
	}

	private JFreeChart createXYBarChart() {
		IntervalXYDataset data = getXYDataset();  // Sets the labels

		JFreeChart chart = ChartFactory.createXYBarChart(
				getName(true),         // chart title
				getDomainLabel(true),               // domain axis label
				true,
				getRangeLabel(true),                  // range axis label
				data,                  // data
				X_AD_Chart.CHARTORIENTATION_Horizontal.equals(getChartOrientation()) 
				? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, // orientation
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
	
		
		return chart;
	}
	
	private JFreeChart createTimeSeriesChart() {
		IntervalXYDataset data = getXYDataset();  // Sets the labels

		JFreeChart chart = ChartFactory.createTimeSeriesChart(
				getName(true),         // chart title
				getDomainLabel(true),               // domain axis label
				getRangeLabel(true),                  // range axis label
				data,                  // data
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
	
		
		return chart;
	}
	
	private JFreeChart createWaterfallChart() {
		CategoryDataset data = getCategoryDataset();  // Sets the labels
		JFreeChart chart = ChartFactory.createWaterfallChart(
				getName(true),         // chart title
				getDomainLabel(true),               // domain axis label
				getRangeLabel(true),                  // range axis label
				data,                  // data
				X_AD_Chart.CHARTORIENTATION_Horizontal.equals(getChartOrientation()) 
					? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, // orientation
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
	
		setupCategoryChart(chart);
		return chart;
	}

	private JFreeChart createRingChart() {
		PieDataset data = getPieDataset(); // Sets name
		final JFreeChart chart = ChartFactory.createRingChart(getName(true),
				data, isDisplayLegend(), true, true);
	
		return chart;
	}

	private JFreeChart createPieChart() {
		PieDataset data = getPieDataset(); // Sets name
		final JFreeChart chart = ChartFactory.createPieChart(getName(true),
				data, false, true, true);
	
		return chart;
	}

	private JFreeChart create3DPieChart() {
		PieDataset data = getPieDataset(); // Sets name
		final JFreeChart chart = ChartFactory
				.createPieChart3D(
						getName(true),
						data, false, true, true);
	
		return chart;
	}

	private JFreeChart createBarChart() {
		CategoryDataset data = getCategoryDataset();  // Sets the labels
		JFreeChart chart = ChartFactory.createBarChart(
				getName(true),         // chart title
				getDomainLabel(true),               // domain axis label
				getRangeLabel(true),                  // range axis label
				data,                  // data
				X_AD_Chart.CHARTORIENTATION_Horizontal.equals(getChartOrientation()) 
				? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, // orientation
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
	
	    BarRenderer renderer = new BarRenderer();
	    renderer.setBarPainter(new StandardBarPainter());
	
		CategoryPlot plot = chart.getCategoryPlot();
		plot.setRenderer(renderer);
		
		setupCategoryChart(chart);
		return chart;
	}

	private JFreeChart create3DBarChart() {
		CategoryDataset data = getCategoryDataset();  // Sets the labels
		JFreeChart chart = ChartFactory.createBarChart(
				getName(true),         // chart title
				getDomainLabel(true),               // domain axis label
				getRangeLabel(true),                  // range axis label
				data,
				X_AD_Chart.CHARTORIENTATION_Horizontal.equals(getChartOrientation()) 
				? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, // orientation
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
		
		setupCategoryChart(chart);
		return chart;
	}

	private JFreeChart createStackedBarChart() {
		CategoryDataset data = getCategoryDataset();  // Sets the labels
		JFreeChart chart = ChartFactory.createStackedBarChart(
				getName(true),         // chart title
				getDomainLabel(true),               // domain axis label
				getRangeLabel(true),                  // range axis label
				data,                  // data
				X_AD_Chart.CHARTORIENTATION_Horizontal.equals(getChartOrientation()) 
				? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, // orientation
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
	
	
	    BarRenderer renderer = new BarRenderer();
	    renderer.setBarPainter(new StandardBarPainter());
	
		CategoryPlot plot = chart.getCategoryPlot();
		plot.setRenderer(renderer);
	    
		setupCategoryChart(chart);
		return chart;
	}

	private JFreeChart create3DStackedBarChart() {
		CategoryDataset data = getCategoryDataset();  // Sets the labels
		JFreeChart chart = ChartFactory.createStackedBarChart(
				getName(true),         // chart title
				getDomainLabel(true),               // domain axis label
				getRangeLabel(true),                  // range axis label
				data,                  // data
				X_AD_Chart.CHARTORIENTATION_Horizontal.equals(getChartOrientation()) 
				? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, // orientation
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
	
		setupCategoryChart(chart);
		return chart;
	}

	private JFreeChart createAreaChart() {
		CategoryDataset data = getCategoryDataset();  // Sets the labels
		// create the chart...
		JFreeChart chart = ChartFactory.createAreaChart(
				getName(true),         // chart title - allow overwrite by the dataset
				getDomainLabel(true),               // domain axis label
				getRangeLabel(true),                  // range axis label
				data,                  // data
				X_AD_Chart.CHARTORIENTATION_Horizontal.equals(getChartOrientation()) 
				? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, // orientation
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
	
		setupCategoryChart(chart);
		return chart;
	}

	private JFreeChart createStackedAreaChart() {
		CategoryDataset data = getCategoryDataset();  // Sets the labels
		// create the chart...
		JFreeChart chart = ChartFactory.createStackedAreaChart(
				getName(true),         // chart title
				getDomainLabel(true),               // domain axis label
				getRangeLabel(true),                  // range axis label
				data,                  // data
				X_AD_Chart.CHARTORIENTATION_Horizontal.equals(getChartOrientation()) 
				? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, // orientation
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
	
		setupCategoryChart(chart);
		return chart;
	}

	private JFreeChart createLineChart() {
		
		CategoryDataset data = getCategoryDataset();
		
		// create the chart...
		JFreeChart chart = ChartFactory.createLineChart(
				getName(true),         // chart title
				getDomainLabel(true),               // domain axis label
				getRangeLabel(true),                  // range axis label
				data,                  // data
				X_AD_Chart.CHARTORIENTATION_Horizontal.equals(getChartOrientation()) 
				? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, // orientation
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
		
	
		setupCategoryChart(chart);
		return chart;
	}

	private JFreeChart create3DLineChart() {
		CategoryDataset data = getCategoryDataset();  // Sets the labels
		// create the chart...
		JFreeChart chart = ChartFactory.createLineChart(
				getName(true),         // chart title
				getDomainLabel(true),             // domain axis label
				getRangeLabel(true),                  // range axis label
				data,                  // data
				X_AD_Chart.CHARTORIENTATION_Horizontal.equals(getChartOrientation()) 
				? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL, // orientation
				isDisplayLegend(),                     // include legend
				true,                     // tooltips?
				true                     // URLs?
		);
		
	
		setupCategoryChart(chart);
		return chart;
	}

	private void setupCategoryChart(JFreeChart chart) {
		CategoryPlot plot = chart.getCategoryPlot();
		
		//  xAxis - Category
		CategoryAxis xAxis = (CategoryAxis)plot.getDomainAxis();
	    xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		//  xAxis - Category
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);
		yAxis.setAutoRange(true);

	    CategoryItemRenderer renderer = plot.getRenderer();
	    renderer.setSeriesPaint(0, Color.RED);
		renderer.setSeriesPaint(1, Color.BLUE);
		renderer.setSeriesPaint(2, Color.YELLOW);
		renderer.setSeriesPaint(3, Color.GREEN);
		renderer.setSeriesPaint(4, Color.ORANGE);
		renderer.setSeriesPaint(5, Color.CYAN);
		renderer.setSeriesPaint(6, Color.MAGENTA);
		renderer.setSeriesPaint(7, Color.GRAY);
		renderer.setSeriesPaint(8, Color.PINK);
		
		plot.setRenderer(renderer);
	}

	/** Get Name, allow overwrite by the data set
	 * @return Alphanumeric identifier of the entity
	 */
	String getName (boolean allowOverWrite) 
	{
		if (allowOverWrite && m_newName != null && !m_newName.isEmpty())
			return m_newName;
		
		return getName();
	}

	String getRangeLabel(boolean allowOverWrite) 
	{
		if (allowOverWrite && m_newRangeLabel != null && !m_newRangeLabel.isEmpty())
			return m_newRangeLabel;
		
		return getRangeLabel();
	}

	String getDomainLabel(boolean allowOverWrite) 
	{
		if (allowOverWrite && m_newDomainLabel != null && !m_newDomainLabel.isEmpty())
			return m_newDomainLabel;
		
		return getDomainLabel();
	}

	void setName(String name, boolean labelOnly) {
		if (labelOnly)
			this.m_newName = name;
		else
			this.setName(name);
	}

	void setRangeLabel(String rangeLabel, boolean labelOnly) {
		if (labelOnly)
			this.m_newRangeLabel = rangeLabel;
		else
			this.setRangeLabel(rangeLabel);
	}

	void setDomainLabel(String domainLabel, boolean labelOnly) {
		if (labelOnly)
			this.m_newDomainLabel = domainLabel;
		else
			this.setDomainLabel(domainLabel);
	}

}
