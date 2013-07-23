/*
 * Copyright (c) 2010 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.edal.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.image.IndexColorModel;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.AbstractXYZDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.domain.impl.HorizontalDomain;
import uk.ac.rdg.resc.edal.coverage.grid.GridCell2D;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.VerticalAxis;
import uk.ac.rdg.resc.edal.coverage.metadata.ScalarMetadata;
import uk.ac.rdg.resc.edal.coverage.metadata.impl.MetadataUtils;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.GridFeature;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.feature.PointSeriesFeature;
import uk.ac.rdg.resc.edal.feature.ProfileFeature;
import uk.ac.rdg.resc.edal.feature.TrajectoryFeature;
import uk.ac.rdg.resc.edal.geometry.impl.LineString;
import uk.ac.rdg.resc.edal.position.GeoPosition;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.VerticalCrs;
import uk.ac.rdg.resc.edal.position.VerticalCrs.PositiveDirection;
import uk.ac.rdg.resc.edal.position.VerticalPosition;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.util.TimeUtils;

/**
 * Code to produce various types of chart. Used by the
 * {@link AbstractWmsController}.
 * 
 * @author Jon Blower
 * @author Kevin X. Yang
 */
final public class Charting {
    private static final Locale US_LOCALE = new Locale("us", "US");
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final DecimalFormat NUMBER_3DP = new DecimalFormat("#0.000");

    public static JFreeChart createTimeseriesPlot(List<Feature> features, String baseMemberName) {
        String yLabel = "";
        VerticalPosition vPos = null;
        ScalarMetadata metadata = null;
        List<TimeSeries> timeSeries = new ArrayList<TimeSeries>();
        for(Feature feature : features) {
            String featureKey;
            List<TimePosition> times;
            List<?> values;
            String memberName = MetadataUtils.getScalarMemberName(feature, baseMemberName);
            if(feature instanceof PointSeriesFeature) {
                PointSeriesFeature pointSeriesFeature = (PointSeriesFeature) feature;
                featureKey = "(" + NUMBER_3DP.format(pointSeriesFeature.getHorizontalPosition().getX()) + ","
                        + NUMBER_3DP.format(pointSeriesFeature.getHorizontalPosition().getY()) + ")";
                times = pointSeriesFeature.getCoverage().getDomain().getTimes();
                values = pointSeriesFeature.getCoverage().getValues(memberName);
                vPos = pointSeriesFeature.getVerticalPosition();
            } else if (feature instanceof TrajectoryFeature) {
                TrajectoryFeature trajectoryFeature = (TrajectoryFeature) feature;
                featureKey = trajectoryFeature.getName();
                List<GeoPosition> domainObjects = trajectoryFeature.getCoverage().getDomain().getDomainObjects();
                times = new ArrayList<TimePosition>();
                for(GeoPosition geoPos : domainObjects){
                    times.add(geoPos.getTimePosition());
                }
                values = trajectoryFeature.getCoverage().getValues(memberName);
            } else {
                continue;
            }
                
            TimeSeries ts = new TimeSeries(featureKey, Millisecond.class);
            
            metadata = MetadataUtils.getScalarMetadata(feature, memberName);
            
            if (!Number.class.isAssignableFrom(feature.getCoverage().getScalarMetadata(memberName)
                    .getValueType())) {
                continue;
            }
            if (times.size() != values.size()) {
                continue;
            }
    
            for (int i = 0; i < times.size(); i++) {
                ts.add(new Millisecond(new Date(times.get(i).getValue())), (Number) values.get(i));
            }
            
            timeSeries.add(ts);
            
            /*
             * TODO There is usually only one feature for this, but we support
             * multiple. Get a better title/y-label
             */
            yLabel = getAxisLabel(feature, memberName);
        }
        
        String title;
        if(metadata != null){
            title = "Timeseries of " + metadata.getTitle();
            if(vPos != null){
                VerticalCrs vCrs = vPos.getCoordinateReferenceSystem();
                String heightOrDepth = "";
                if(vCrs != null) {
                    heightOrDepth = (vCrs.getPositiveDirection() == PositiveDirection.UP) ? " high" : " deep";
                }
                title += " at " + vPos + heightOrDepth;
            }
        } else {
            title = "No data";
        }
        
        TimeSeriesCollection xydataset = new TimeSeriesCollection();
        
        for(TimeSeries ts : timeSeries) {
            xydataset.addSeries(ts);
        }
        
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Date / time", yLabel,
                xydataset, (timeSeries.size() > 1), false, false);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        for(int i=0; i<timeSeries.size(); i++){
            renderer.setSeriesShape(i, new Ellipse2D.Double(-1.0, -1.0, 2.0, 2.0));
            renderer.setSeriesShapesVisible(i, true);
        }
        chart.getXYPlot().setRenderer(renderer);
        chart.getXYPlot().setNoDataMessage("There is no data for your choice");
        chart.getXYPlot().setNoDataMessageFont(new Font("sansserif", Font.BOLD, 32));
        
        return chart;
    }

    public static JFreeChart createVerticalProfilePlot(List<ProfileFeature> features, String baseMemberName) {
        XYSeriesCollection xySeriesColl = new XYSeriesCollection();
        
        String xAxisLabel = "";
        TimePosition dateTime = null;
        ScalarMetadata metadata = null;
        boolean invertYAxis = false;
        ZAxisAndValues zAxisAndValues = null;
        for(ProfileFeature feature : features) {
            String memberName = MetadataUtils.getScalarMemberName(feature, baseMemberName);
            metadata = MetadataUtils.getScalarMetadata(feature, memberName);
            
            if (!Number.class.isAssignableFrom(feature.getCoverage().getScalarMetadata(memberName)
                    .getValueType())) {
                continue;
            }
    
            /*
             * We can do this conversion, because we have already thrown an
             * exception if it's invalid
             */
            @SuppressWarnings("unchecked")
            List<Number> dataValues = (List<Number>) feature.getCoverage().getValues(memberName);
            dateTime = feature.getTime();

            List<Double> elevationValues = feature.getCoverage().getDomain().getZValues();
            zAxisAndValues = getZAxisAndValues(elevationValues, feature.getCoverage().getDomain().getVerticalCrs());
    
            /*
             * This is the label used for the legend. We *may* want to include
             * the feature ID, but generally speaking that will be rather
             * arbitrary
             */
            String location = "(" + NUMBER_3DP.format(feature.getHorizontalPosition().getX()) + ","
                    + NUMBER_3DP.format(feature.getHorizontalPosition().getY()) + ")";
            XYSeries series = new XYSeries(location, true);
            series.setDescription(memberName);
            for (int i = 0; i < elevationValues.size(); i++) {
                Number val = dataValues.get(i);
                if (val.equals(Float.NaN) || val.equals(Double.NaN)) {
                    /*
                     * Don't add NaNs to the series
                     */
                    continue;
                }
                series.add(elevationValues.get(i), dataValues.get(i));
            }
            
            xAxisLabel = getAxisLabel(feature, memberName);
            
            xySeriesColl.addSeries(series);
        }
        
        NumberAxis elevationAxis = null;
        if(zAxisAndValues != null) {
            elevationAxis = zAxisAndValues.zAxis;
        }
        elevationAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        if (invertYAxis)
            elevationAxis.setInverted(true);
        elevationAxis.setAutoRangeIncludesZero(false);
        
        NumberAxis valueAxis = new NumberAxis(xAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for(int i=0; i<features.size(); i++){
            renderer.setSeriesShape(i, new Ellipse2D.Double(-1.0, -1.0, 2.0, 2.0));
            renderer.setSeriesShapesVisible(i, true);
        }

        XYPlot plot = new XYPlot(xySeriesColl, elevationAxis, valueAxis, renderer);
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(Color.white);
        plot.setOrientation(PlotOrientation.HORIZONTAL);

        String title;
        if(metadata != null){
            title = "Profile of " + metadata.getTitle();
            if (dateTime != null) {
                title += " at " + TimeUtils.formatUTCHumanReadableDateTime(dateTime);
            }
        } else {
            title = "No data";
        }
        
        /*
         * Use default font and create a legend
         */
        return new JFreeChart(title, null, plot, true);
    }

    private static String getAxisLabel(Feature feature, String memberName) {
        ScalarMetadata metadata = MetadataUtils.getScalarMetadata(feature, memberName);
        return metadata.getTitle() + " (" + metadata.getUnits().getUnitString() + ")";
    }

    public static JFreeChart createTransectPlot(GridFeature feature, String memberName,
            LineString transectDomain, String copyrightStatement, boolean hasVerticalAxis) {

        if (!Number.class.isAssignableFrom(feature.getCoverage().getScalarMetadata(memberName)
                .getValueType())) {
            throw new IllegalArgumentException("Cannot plot a transect for a non-numerical layer");
        }

        JFreeChart chart;
        XYPlot plot;
        XYSeries series = new XYSeries("data", true);

        HorizontalDomain optimalTransectDomain = getOptimalTransectDomain(feature.getCoverage().getDomain(), transectDomain);

        int k = 0;
        for (HorizontalPosition pos : optimalTransectDomain.getDomainObjects()) {
            series.add(k++, (Number) feature.getCoverage().evaluate(pos, memberName));
        }

        XYSeriesCollection xySeriesColl = new XYSeriesCollection();
        xySeriesColl.addSeries(series);

        /*
         * If we have a layer with more than one elevation value, we create a
         * transect chart using standard XYItem Renderer to keep the plot
         * renderer consistent with that of vertical section plot
         */
        if (hasVerticalAxis) {
            final XYItemRenderer renderer1 = new StandardXYItemRenderer();
            final NumberAxis rangeAxis1 = new NumberAxis(getAxisLabel(feature, memberName));
            plot = new XYPlot(xySeriesColl, new NumberAxis(), rangeAxis1, renderer1);
            plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
            plot.setBackgroundPaint(Color.lightGray);
            plot.setDomainGridlinesVisible(false);
            plot.setRangeGridlinePaint(Color.white);
            plot.getRenderer().setSeriesPaint(0, Color.RED);
            plot.setOrientation(PlotOrientation.VERTICAL);
            chart = new JFreeChart(plot);
        } else {
            /*
             * If we have a layer which only has one elevation value, we simply
             * create XY Line chart
             */
            String yLabel = getAxisLabel(feature, memberName);
            chart = ChartFactory.createXYLineChart(
                    MetadataUtils.getScalarMetadata(feature, memberName).getTitle(),
                    "distance along transect (arbitrary units)", yLabel, xySeriesColl,
                    PlotOrientation.VERTICAL, false, false, false);
            plot = chart.getXYPlot();
        }
        if (copyrightStatement != null && !hasVerticalAxis) {
            final TextTitle textTitle = new TextTitle(copyrightStatement);
            textTitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
            textTitle.setPosition(RectangleEdge.BOTTOM);
            textTitle.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            chart.addSubtitle(textTitle);
        }
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        rangeAxis.setAutoRangeIncludesZero(false);
        plot.setNoDataMessage("There is no data for what you have chosen.");

        // Iterate through control points to show segments of transect
        Double prevCtrlPointDistance = null;
        for (int i = 0; i < transectDomain.getControlPoints().size(); i++) {
            double ctrlPointDistance = transectDomain.getFractionalControlPointDistance(i);
            if (prevCtrlPointDistance != null) {
                // determine start end end value for marker based on index of
                // ctrl point
                int size = optimalTransectDomain.getDomainObjects().size();
                IntervalMarker target = new IntervalMarker(size * prevCtrlPointDistance, size
                        * ctrlPointDistance);
                // TODO: printing to two d.p. not always appropriate
                target.setLabel("["
                        + printTwoDecimals(transectDomain.getControlPoints().get(i - 1).getY())
                        + ","
                        + printTwoDecimals(transectDomain.getControlPoints().get(i - 1).getX())
                        + "]");
                target.setLabelFont(new Font("SansSerif", Font.ITALIC, 11));
                // alter color of segment and position of label based on
                // odd/even index
                if (i % 2 == 0) {
                    target.setPaint(new Color(222, 222, 255, 128));
                    target.setLabelAnchor(RectangleAnchor.TOP_LEFT);
                    target.setLabelTextAnchor(TextAnchor.TOP_LEFT);
                } else {
                    target.setPaint(new Color(233, 225, 146, 128));
                    target.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
                    target.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
                }
                // add marker to plot
                plot.addDomainMarker(target);
            }
            prevCtrlPointDistance = transectDomain.getFractionalControlPointDistance(i);
        }

        return chart;
    }

    /**
     * Gets a HorizontalDomain that contains (near) the minimum necessary number
     * of points to sample a layer's source grid of data. That is to say,
     * creating a HorizontalDomain at higher resolution would not result in
     * sampling significantly more points in the layer's source grid.
     * 
     * @param feature
     *            The feature for which the transect will be generated
     * @param transect
     *            The transect as specified in the request
     * @return a HorizontalDomain that contains (near) the minimum necessary
     *         number of points to sample a layer's source grid of data.
     */
    public static HorizontalDomain getOptimalTransectDomain(HorizontalGrid hGrid,
            LineString transect) {
        // We need to work out how many points we need to include in order to
        // completely sample the data grid (i.e. we need the resolution of the
        // points to be higher than that of the data grid). It's hard to work
        // this out neatly (data grids can be irregular) but we can estimate
        // this by creating transects at progressively higher resolution, and
        // working out how many grid points will be sampled.
        int numTransectPoints = 500; // a bit more than the final image width
        int lastNumUniqueGridPointsSampled = -1;
        HorizontalDomain pointList = null;
        while (true) {
            // Create a transect with the required number of points,
            // interpolating
            // between the control points in the line string
            List<HorizontalPosition> points = transect.getPointsOnPath(numTransectPoints);
            // Create a HorizontalDomain from the interpolated points
            HorizontalDomain testPointList = new HorizontalDomain(points);

            /*
             * Work out how many grid points will be sampled by this transect
             * Relies on equals() being implemented correctly for the
             * GridCoordinates
             */
            Set<GridCell2D> gridCoords = new HashSet<GridCell2D>();
            for (HorizontalPosition pos : testPointList.getDomainObjects()) {
                GridCell2D gridCoord = hGrid.findContainingCell(pos);
                if (gridCoord != null)
                    gridCoords.add(hGrid.findContainingCell(pos));
            }

            int numUniqueGridPointsSampled = gridCoords.size();

            // If this increase in resolution results in at least 10% more
            // points
            // being sampled we'll go around the loop again
            if (numUniqueGridPointsSampled > lastNumUniqueGridPointsSampled * 1.1) {
                // We need to increase the transect resolution and try again
                lastNumUniqueGridPointsSampled = numUniqueGridPointsSampled;
                numTransectPoints += 500;
                pointList = testPointList;
            } else {
                // We've gained little advantage by the last resolution increase
                return pointList;
            }
        }
    }

    /**
     * Prints a double-precision number to 2 decimal places
     * 
     * @param d
     *            the double
     * @return rounded value to 2 places, as a String
     */
    private static String printTwoDecimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        // We need to set the Locale properly, otherwise the DecimalFormat
        // doesn't
        // work in locales that use commas instead of points.
        // Thanks to Justino Martinez for this fix!
        DecimalFormatSymbols decSym = DecimalFormatSymbols.getInstance(US_LOCALE);
        twoDForm.setDecimalFormatSymbols(decSym);
        return twoDForm.format(d);
    }

    /**
     * Simple class to hold a z axis and its values for a vertical section or
     * profile plot
     */
    private static final class ZAxisAndValues {
        private final NumberAxis zAxis;
        private final List<Double> zValues;

        public ZAxisAndValues(NumberAxis zAxis, List<Double> zValues) {
            this.zAxis = zAxis;
            this.zValues = zValues;
        }
    }

    /**
     * Creates a vertical axis for plotting the given elevation values from the
     * given layer
     */
    private static ZAxisAndValues getZAxisAndValues(List<Double> elevationValues, VerticalCrs vCrs) {
        /*
         * We can deal with three types of vertical axis: Height, Depth and
         * Pressure. The code for this is very messy in ncWMS, sorry about
         * that... We should improve this but there are possible knock-on
         * effects, so it's not a very easy job.
         */
        final String zAxisLabel;
        final boolean invertYAxis;
        if (vCrs.getPositiveDirection() == PositiveDirection.UP) {
            zAxisLabel = "Height";
            invertYAxis = false;
        } else if (vCrs.isPressure()) {
            zAxisLabel = "Pressure";
            invertYAxis = true;
        } else {
            zAxisLabel = "Depth";
            invertYAxis = true;
        }

        NumberAxis zAxis = new NumberAxis(zAxisLabel + " (" + vCrs.getUnits().getUnitString() + ")");
        zAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        zAxis.setInverted(invertYAxis);

        return new ZAxisAndValues(zAxis, elevationValues);
    }

    /**
     * Creates and returns a vertical section chart.
     * 
     * @param feature
     *            The feature containing the data
     * @param horizPath
     *            The horizontal path described by the vertical section
     * @param elevationValues
     *            The elevation values for which we have data
     * @param sectionData
     *            The data values for the section data. Each List&lt;Float&gt
     *            contains the values for each point on the horizontal path for
     *            one of the elevation values.
     * @return
     */
    public static JFreeChart createVerticalSectionChart(GridSeriesFeature feature,
            String memberName, LineString horizPath, Extent<Float> colourScaleRange,
            ColorPalette palette, int numColourBands, boolean logarithmic, VerticalPosition zValue,
            TimePosition time) {

        if (feature.getCoverage().getDomain().getVerticalAxis() == null) {
            throw new IllegalArgumentException(
                    "Cannot create a vertical section chart from a feature with no vertical axis");
        }
        if (!Number.class.isAssignableFrom(feature.getCoverage().getScalarMetadata(memberName)
                .getValueType())) {
            throw new IllegalArgumentException(
                    "Cannot create a vertical section chart from a non-numeric field");
        }

        VerticalAxis vAxis = feature.getCoverage().getDomain().getVerticalAxis();
        ZAxisAndValues zAxisAndValues = getZAxisAndValues(vAxis.getCoordinateValues(), vAxis.getVerticalCrs());
        List<Double> elevationValues = zAxisAndValues.zValues;

        double minElValue = 0.0;
        double maxElValue = 1.0;

        List<List<Number>> sectionData = new ArrayList<List<Number>>();
        HorizontalDomain optimalTransectDomain = getOptimalTransectDomain(feature.getCoverage()
                .getDomain().getHorizontalGrid(), horizPath);
        List<HorizontalPosition> controlPoints = optimalTransectDomain.getDomainObjects();
        for (HorizontalPosition pos : controlPoints) {
            /*
             * This cast is OK, because we have already thrown an exception if
             * this doesn't return a number
             */
            @SuppressWarnings("unchecked")
            List<Number> values = (List<Number>) feature
                    .extractProfileFeature(pos, time, CollectionUtils.setOf(memberName))
                    .getCoverage().getValues(memberName);
            sectionData.add(values);
        }

        if (elevationValues.size() > 0 && controlPoints.size() > 0) {
            minElValue = elevationValues.get(0);
            maxElValue = elevationValues.get(elevationValues.size() - 1);
        }

        // Sometimes values on the axes are reversed
        if (minElValue > maxElValue) {
            double temp = minElValue;
            minElValue = maxElValue;
            maxElValue = temp;
        }

        // TODO expand the minElValue and maxElValue a bit

        // The number of elevation values that will be represented in the final
        // dataset. TODO: calculate this based on the minimum elevation spacing
        int numElValues = 300;

        XYZDataset dataset = new VerticalSectionDataset(elevationValues, sectionData, minElValue,
                maxElValue, numElValues);

        NumberAxis xAxis = new NumberAxis("Distance along path (arbitrary units)");
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        PaintScale scale = createPaintScale(palette, colourScaleRange, numColourBands, logarithmic);

        NumberAxis colorScaleBar = new NumberAxis();
        org.jfree.data.Range colorBarRange = new org.jfree.data.Range(colourScaleRange.getLow(),
                colourScaleRange.getHigh());
        colorScaleBar.setRange(colorBarRange);

        PaintScaleLegend paintScaleLegend = new PaintScaleLegend(scale, colorScaleBar);
        paintScaleLegend.setPosition(RectangleEdge.BOTTOM);

        XYBlockRenderer renderer = new XYBlockRenderer();
        double elevationResolution = (maxElValue - minElValue) / numElValues;
        renderer.setBlockHeight(elevationResolution);
        renderer.setPaintScale(scale);

        XYPlot plot = new XYPlot(dataset, xAxis, zAxisAndValues.zAxis, renderer);
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(Color.white);

        // Iterate through control points to show segments of transect
        Double prevCtrlPointDistance = null;
        int xAxisLength = 0;
        if (sectionData.size() > 0)
            xAxisLength = sectionData.size();
        for (int i = 0; i < horizPath.getControlPoints().size(); i++) {
            double ctrlPointDistance = horizPath.getFractionalControlPointDistance(i);
            if (prevCtrlPointDistance != null) {
                // determine start end end value for marker based on index of
                // ctrl point
                IntervalMarker target = new IntervalMarker(xAxisLength * prevCtrlPointDistance,
                        xAxisLength * ctrlPointDistance);
                target.setPaint(TRANSPARENT);
                // add marker to plot
                plot.addDomainMarker(target);
                // add line marker to vertical section plot
                final Marker verticalLevel = new ValueMarker(Math.abs(zValue.getZ()));
                verticalLevel.setPaint(Color.lightGray);
                verticalLevel.setLabel("at " + zValue + "  level ");
                verticalLevel.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
                verticalLevel.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
                plot.addRangeMarker(verticalLevel);

            }
            prevCtrlPointDistance = horizPath.getFractionalControlPointDistance(i);
        }

        JFreeChart chart = new JFreeChart(feature.getName() + " ("
                + feature.getCoverage().getScalarMetadata(memberName).getUnits().getUnitString() + ")",
                plot);
        chart.removeLegend();
        chart.addSubtitle(paintScaleLegend);
        chart.setBackgroundPaint(Color.white);
        return chart;
    }

    /**
     * An {@link XYZDataset} that is created by interpolating a set of values
     * from a discrete set of elevations.
     */
    private static class VerticalSectionDataset extends AbstractXYZDataset {
        private static final long serialVersionUID = 1L;
        private final int horizPathLength;
        private final List<List<Number>> sectionData;
        private final List<Double> elevationValues;
        private final double minElValue;
        private final double elevationResolution;
        private final int numElevations;

        public VerticalSectionDataset(List<Double> elevationValues, List<List<Number>> sectionData,
                double minElValue, double maxElValue, int numElevations) {
            /*
             * TODO Test that this is the right way round
             */
            if (sectionData.size() > 0)
                this.horizPathLength = sectionData.size();
            else
                this.horizPathLength = 0;
            this.sectionData = sectionData;
            this.elevationValues = elevationValues;
            this.minElValue = minElValue;
            this.numElevations = numElevations;
            this.elevationResolution = (maxElValue - minElValue) / numElevations;
        }

        @Override
        public int getSeriesCount() {
            return 1;
        }

        @Override
        public String getSeriesKey(int series) {
            checkSeries(series);
            return "Vertical section";
        }

        @Override
        public int getItemCount(int series) {
            checkSeries(series);
            return this.horizPathLength * this.numElevations;
        }

        @Override
        public Integer getX(int series, int item) {
            checkSeries(series);
            // The x coordinate is just the integer index of the point along
            // the horizontal path
            return item % this.horizPathLength;
        }

        /**
         * Gets the value of elevation, assuming linear variation between min
         * and max.
         */
        @Override
        public Double getY(int series, int item) {
            checkSeries(series);
            int yIndex = item / this.horizPathLength;
            return this.minElValue + yIndex * this.elevationResolution;
        }

        /**
         * Gets the data value corresponding with the given item, interpolating
         * between the recorded data values using nearest-neighbour
         * interpolation
         */
        @Override
        public Float getZ(int series, int item) {
            checkSeries(series);
            int xIndex = item % this.horizPathLength;
            double elevation = this.getY(series, item);
            // What is the index of the nearest elevation in the list of
            // elevations
            // for which we have data?
            // TODO: factor this out into a utility routine
            int nearestElevationIndex = -1;
            double minDiff = Double.MAX_VALUE;
            for (int i = 0; i < this.elevationValues.size(); i++) {
                double el = this.elevationValues.get(i);
                double diff = Math.abs(el - elevation);
                if (diff < minDiff) {
                    minDiff = diff;
                    nearestElevationIndex = i;
                }
            }
            return sectionData.get(xIndex).get(nearestElevationIndex).floatValue();
        }

        /**
         * @throws IllegalArgumentException
         *             if the argument is not zero.
         */
        private static void checkSeries(int series) {
            if (series != 0)
                throw new IllegalArgumentException("Series must be zero");
        }
    }

    /**
     * Creates and returns a JFreeChart {@link PaintScale} that converts data
     * values to {@link Color}s.
     */
    public static PaintScale createPaintScale(ColorPalette colorPalette,
            final Extent<Float> colourScaleRange, final int numColourBands,
            final boolean logarithmic) {
        final IndexColorModel cm = colorPalette.getColorModel(numColourBands, 100);

        return new PaintScale() {
            @Override
            public double getLowerBound() {
                return colourScaleRange.getLow();
            }

            @Override
            public double getUpperBound() {
                return colourScaleRange.getHigh();
            }

            @Override
            public Color getPaint(double value) {
                // TODO: replicate/factor out code in ImageProducer.java
                int index = this.getColourIndex(value);
                return new Color(cm.getRGB(index));
            }

            /**
             * @return the colour index that corresponds to the given value
             * @todo This is adapted from ImageProducer.
             */
            private int getColourIndex(double value) {
                if (Double.isNaN(value)) {
                    return numColourBands; // represents a background pixel
                } else if (value < this.getLowerBound() || value > this.getUpperBound()) {
                    return numColourBands + 1; // represents an out-of-range
                    // pixel
                } else {
                    double min = logarithmic ? Math.log(this.getLowerBound()) : this
                            .getLowerBound();
                    double max = logarithmic ? Math.log(this.getUpperBound()) : this
                            .getUpperBound();
                    double val = logarithmic ? Math.log(value) : value;
                    double frac = (val - min) / (max - min);
                    // Compute and return the index of the corresponding colour
                    int index = (int) (frac * numColourBands);
                    /*
                     * For values very close to the maximum value in the range,
                     * this index might turn out to be equal to
                     * this.numColourBands due to rounding error. In this case
                     * we subtract one from the index to ensure that such pixels
                     * are not displayed as background pixels.
                     */
                    if (index == numColourBands)
                        index--;
                    return index;
                }
            }
        };
    }

}
