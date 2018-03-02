package org.sakaiproject.gradebookng.tool.panels;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.gradebookng.business.GradebookNgBusinessService;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.tool.pages.GradebookPage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberTickUnitSource;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CourseGrade;

/**
 *
 * Panel for the modal window that shows course grade statistics
 *
 */
public class CourseGradeStatisticsPanel extends Panel {

    private static final long serialVersionUID = 1L;

    @SpringBean(name = "org.sakaiproject.gradebookng.business.GradebookNgBusinessService")
    protected GradebookNgBusinessService businessService;

    private final ModalWindow window;

    public CourseGradeStatisticsPanel(final String id, final ModalWindow window) {
        super(id);
        this.window = window;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        CourseGradeStatisticsPanel.this.window.setTitle(
                (new StringResourceModel("coursegrade.statistics.title", null, null).getString()));

        List<String> studentIds = new ArrayList<String>();
        final Map<String, CourseGrade> courseGrades = this.businessService.getCourseGrades(studentIds);
        List<Double> grades = new ArrayList<Double>();
        Iterator it = courseGrades.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            CourseGrade cg = (CourseGrade) pair.getValue();
            if(null != cg.getCalculatedGrade())
                grades.add(Double.parseDouble(cg.getCalculatedGrade()));
        }

        Collections.sort(grades);

        final DefaultCategoryDataset data = new DefaultCategoryDataset();

        final SortedMap<String, Integer> counts = new TreeMap();

        // Start off with a 0-50% range
        counts.put(String.format("%d-%d", 0, 50), 0);

        final int range = 10;
        for (int start = 50; start < 100; start = start + range) {
            final String key = String.format("%d-%d", start, start + range);
            counts.put(key, 0);
        }
        int extraCredits = 0;
        for (final Double grade : grades) {
            if (grade > 100) {
              extraCredits = extraCredits + 1;
              continue;
            }
            final int total = Double.valueOf(grade / range).intValue();
            int start = total * range;

            if (start == 100) {
                start = start - range;
            }

            String key = String.format("%d-%d", start, start + range);

            if (start < 50) {
                key = String.format("%d-%d", 0, 50);
            }

            counts.put(key, counts.get(key) + 1);
        }

        for (final String label : counts.keySet()) {
            data.addValue(counts.get(label), "count", label);
        }

        if (extraCredits > 0) {
          data.addValue(extraCredits, "count",
          getString("label.statistics.chart.extracredit"));
        }

        //make bar graph
        final JFreeChart chart = ChartFactory.createBarChart(
                null, // the chart title
                getString("coursegrade.statistics.chart.xaxis"), // the label for the category axis
                getString("coursegrade.statistics.chart.yaxis"), // the label for the value axis
                data, // the dataset for the chart
                PlotOrientation.VERTICAL, // the plot orientation
                false, // show legend
                true, // show tooltips
                false); // show urls

        chart.setBorderVisible(false);

        chart.setAntiAlias(false);

        final CategoryPlot categoryPlot = chart.getCategoryPlot();
        final BarRenderer br = (BarRenderer) categoryPlot.getRenderer();

        br.setItemMargin(0);
        br.setMinimumBarLength(0.05);
        br.setMaximumBarWidth(0.1);
        br.setSeriesPaint(0, new Color(51, 122, 183));
        br.setBarPainter(new StandardBarPainter());
        br.setShadowPaint(new Color(220, 220, 220));
        BarRenderer.setDefaultShadowsVisible(true);

        br.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator(
                getString("label.statistics.chart.tooltip"), NumberFormat.getInstance()));

        categoryPlot.setRenderer(br);

        // show only integers in the count axis
        categoryPlot.getRangeAxis().setStandardTickUnits(new NumberTickUnitSource(true));
        categoryPlot.setBackgroundPaint(Color.white);

        add(new JFreeChartImageWithToolTip("chart", Model.of(chart), "tooltip", 540, 300));

        if(grades.size() > 0) {
            add(new Label("average", constructAverageLabel(grades)));
            add(new Label("median", constructMedianLabel(grades)));
            add(new Label("graded", String.valueOf(courseGrades.size())));
            add(new Label("deviation", constructStandardDeviationLabel(grades)));
            add(new Label("lowest", constructLowestLabel(grades)));
            add(new Label("highest", constructHighestLabel(grades)));
        }
        else {
            add(new Label("average", "-"));
            add(new Label("median", "-"));
            add(new Label("graded", "-"));
            add(new Label("deviation", "-"));
            add(new Label("lowest", "-"));
            add(new Label("highest", "-"));
        }
        final GbAjaxButton done = new GbAjaxButton("done") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                CourseGradeStatisticsPanel.this.window.close(target);
            }
        };
        done.setDefaultFormProcessing(false);
        add(done);
    }

    private String constructAverageLabel(final List<Double> courseGrades) {
        final double average = calculateAverage(courseGrades);
        final String percentage = FormatHelper.formatDoubleAsPercentage(average);
        return percentage;
    }

    private String constructMedianLabel(final List<Double> courseGrades) {
        final double median = calculateMedian(courseGrades);
        final String percentage = FormatHelper.formatDoubleAsPercentage(median);
        return percentage;
    }

    private String constructStandardDeviationLabel(final List<Double> courseGrades) {
        final double deviation = calculateStandardDeviation(courseGrades);
        return FormatHelper.formatDoubleToDecimal(Double.valueOf(deviation));
    }

    private String constructLowestLabel(final List<Double> courseGrades) {
        final double lowest = Collections.min(courseGrades);
        final String percentage = FormatHelper.formatDoubleAsPercentage(lowest);
        return percentage;
    }

    private String constructHighestLabel(final List<Double> courseGrades) {
        final double highest = Collections.max(courseGrades);
        final String percentage = FormatHelper.formatDoubleAsPercentage(highest);
        return percentage;
    }

    private double calculateAverage(List<Double> grades) {
        double sum = 0;
        for (int i = 0; i < grades.size(); i++) {
            sum += grades.get(i);
        }
        return sum / grades.size();
    }

    private double calculateMedian(List<Double> grades) {
        int middle = grades.size() / 2;
        if (grades.size() % 2 == 1) {
            return grades.get(middle);
        } else {
            return (grades.get(middle - 1) + grades.get(middle)) / 2.0;
        }
    }

    private double calculateVariance(final List<Double> allGrades) {
        final double mean = calculateAverage(allGrades);
        double sum = 0;

        for (int i = 0; i < allGrades.size(); i++) {
            final double grade = allGrades.get(i);
            sum += (mean - grade) * (mean - grade);
        }

        return sum / allGrades.size();
    }

    private double calculateStandardDeviation(final List<Double> allGrades) {
        return Math.sqrt(calculateVariance(allGrades));
    }
}

// class JFreeChartImageWithToolTip extends NonCachingImage {
//     private final String imageMapId;
//     private final int width;
//     private final int height;
//     private final ChartRenderingInfo chartRenderingInfo = new ChartRenderingInfo(new StandardEntityCollection());

//     public JFreeChartImageWithToolTip(final String id, final IModel<JFreeChart> model,
//             final String imageMapId, final int width, final int height) {
//         super(id, model);
//         this.imageMapId = imageMapId;
//         this.width = width;
//         this.height = height;
//     }

//     @Override
//     protected IResource getImageResource() {
//         IResource imageResource = null;
//         final JFreeChart chart = (JFreeChart) getDefaultModelObject();
//         imageResource = new DynamicImageResource() {
//             @Override
//             protected byte[] getImageData(final Attributes attributes) {
//                 final ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                 try {
//                     if (chart != null) {
//                         JFreeChartImageWithToolTip.this.chartRenderingInfo.clear();
//                         ChartUtilities.writeChartAsPNG(stream, chart, JFreeChartImageWithToolTip.this.width,
//                                 JFreeChartImageWithToolTip.this.height, JFreeChartImageWithToolTip.this.chartRenderingInfo);
//                     }
//                 } catch (final IOException ex) {
//                     // TODO logging for rendering chart error
//                 }
//                 return stream.toByteArray();
//             }
//         };
//         return imageResource;
//     }

//     @Override
//     public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
//         final JFreeChart chart = (JFreeChart) getDefaultModelObject();
//         if (chart == null) {
//             return;
//         }
//         final ByteArrayOutputStream stream = new ByteArrayOutputStream();
//         try {
//             this.chartRenderingInfo.clear();
//             ChartUtilities.writeChartAsPNG(stream, chart, this.width, this.height, this.chartRenderingInfo);
//         } catch (final IOException ex) {
//             // do something
//         }
//         replaceComponentTagBody(markupStream, openTag, ChartUtilities.getImageMap(this.imageMapId, this.chartRenderingInfo));
//     }
// }
