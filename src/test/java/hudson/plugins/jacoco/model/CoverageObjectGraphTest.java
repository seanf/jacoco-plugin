package hudson.plugins.jacoco.model;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.jfree.chart.JFreeChart;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import hudson.plugins.jacoco.AbstractJacocoTestBase;
import hudson.plugins.jacoco.model.CoverageGraphLayout.CoverageType;
import hudson.plugins.jacoco.model.CoverageGraphLayout.CoverageValue;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * @author Martin Heinzerling
 */
public class CoverageObjectGraphTest extends AbstractJacocoTestBase
{
	public static final int WIDTH = 500;
	public static final int HEIGHT = 200;
	private static Font font;
	private IMocksControl ctl;
	private Locale localeBackup;

	@BeforeClass
	public static void loadFont() throws IOException, FontFormatException
	{
		// just a free font nobody has on their system, but different enough to default sans-serif,
		// that you will see missing system font replacement in the output. See #replaceFonts()
		InputStream is = new BufferedInputStream(new FileInputStream(
				"resources/test/belligerent.ttf"));
		try {
			font = Font.createFont(Font.TRUETYPE_FONT, is);
		} finally {
			is.close();
		}
	}

	@Before
	public void setUp()
	{
		ctl = EasyMock.createControl();
		TestCoverageObject.setEasyMock(ctl);
		localeBackup=Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);
	}

	@After
	public void tearDown()
	{
		ctl.verify();
		TestCoverageObject.setEasyMock(null);
		Locale.setDefault(localeBackup);
	}

	@Test
	public void simpleLineCoverage() throws IOException
	{
		CoverageGraphLayout layout = new CoverageGraphLayout()
				/*.baseStroke(4f)*/
				.plot().type(CoverageType.LINE).value(CoverageValue.MISSED).color(Color.RED)
				.plot().type(CoverageType.LINE).value(CoverageValue.COVERED).color(Color.GREEN);

		JFreeChart chart = createTestCoverage().createGraph(new GregorianCalendar(), WIDTH, HEIGHT, layout).getGraph();
		assertGraph(chart, "simple.svg");
	}

	@Test
	public void baseStroke() throws IOException
	{
		CoverageGraphLayout layout = new CoverageGraphLayout().
				baseStroke(2.0f)
				.plot().type(CoverageType.LINE).value(CoverageValue.MISSED).color(Color.RED)
				.plot().type(CoverageType.LINE).value(CoverageValue.COVERED).color(Color.GREEN);

		JFreeChart chart = createTestCoverage().createGraph(new GregorianCalendar(), WIDTH, HEIGHT, layout).getGraph();
		assertGraph(chart, "baseStroke.svg");
	}

	@Test
	public void multipleAccessAndDifferentCoverageType() throws IOException
	{
		CoverageGraphLayout layout = new CoverageGraphLayout()
				.baseStroke(2f)
				.axis().label("M")
				.plot().type(CoverageType.LINE).value(CoverageValue.MISSED).color(Color.RED)
				.axis().label("C")
				.plot().type(CoverageType.LINE).value(CoverageValue.COVERED).color(Color.GREEN)
				.axis().label("%")
				.plot().type(CoverageType.BRANCH).value(CoverageValue.PERCENTAGE).color(Color.BLUE)
				.plot().type(CoverageType.LINE).value(CoverageValue.PERCENTAGE).color(Color.YELLOW);

		JFreeChart chart = createTestCoverage().createGraph(new GregorianCalendar(), WIDTH, HEIGHT, layout).getGraph();
		assertGraph(chart, "multiple.svg");
	}

	@Test
	public void crop5() throws IOException
	{
		CoverageGraphLayout layout = new CoverageGraphLayout()
				.baseStroke(2f)
				.axis().crop(5).skipZero()
				.plot().type(CoverageType.BRANCH).value(CoverageValue.PERCENTAGE).color(Color.RED);

		JFreeChart chart = createTestCoverage().createGraph(new GregorianCalendar(), WIDTH, HEIGHT, layout).getGraph();
		assertGraph(chart, "crop5.svg");
	}

	@Test
	public void crop100() throws IOException
	{
		CoverageGraphLayout layout = new CoverageGraphLayout()
				.baseStroke(2f)
				.axis().crop(100).skipZero()
				.plot().type(CoverageType.BRANCH).value(CoverageValue.PERCENTAGE).color(Color.RED);

		JFreeChart chart = createTestCoverage().createGraph(new GregorianCalendar(), WIDTH, HEIGHT, layout).getGraph();
		assertGraph(chart, "crop100.svg");
	}

	@Test
	public void skipZero() throws IOException
	{
		CoverageGraphLayout layout = new CoverageGraphLayout()
				.skipZero()
				.plot().type(CoverageType.BRANCH).value(CoverageValue.PERCENTAGE).color(Color.RED);

		JFreeChart chart = createTestCoverage().createGraph(new GregorianCalendar(), WIDTH, HEIGHT, layout).getGraph();
		assertGraph(chart, "skipzero.svg");
	}

	private TestCoverageObject createTestCoverage()
	{
		TestCoverageObject t5 = new TestCoverageObject().branch(6, 30).line(5000, 19000);
		TestCoverageObject t4 = new TestCoverageObject().branch(6, 0).line(5000, 19000).previous(t5);
		TestCoverageObject t3 = new TestCoverageObject().branch(6, 35).line(5000, 19000).previous(t4);
		TestCoverageObject t2 = new TestCoverageObject().branch(15, 23).line(10000, 15000).previous(t3);
		TestCoverageObject t1 = new TestCoverageObject().branch(27, 13).line(12000, 18000).previous(t2);
		TestCoverageObject t0 = new TestCoverageObject().previous(t1);
		ctl.replay();
		return t0;
	}

	private void assertGraph(JFreeChart chart, String file, boolean writeFile) throws IOException
	{
		File outputFile = new File(file).getAbsoluteFile();
		replaceFonts(chart);
		File expectedFile = new File("resources/test/" + file).getAbsoluteFile();
		String actual = saveChartAsSVG(chart, WIDTH, HEIGHT);
		if (writeFile)
		{
			FileUtils.writeStringToFile(outputFile, actual, "UTF-8");
			System.out.println("Stored graph file to " + outputFile.getAbsolutePath());
		}
		String expected = FileUtils.readFileToString(expectedFile);

		try
		{
			assertEquals(expected, actual);
		}
		catch (AssertionError e)
		{
			FileUtils.writeStringToFile(outputFile, actual, "UTF-8");
			System.err.println("Stored wrong graph file to " + outputFile.getAbsolutePath());
			throw e;
		}
	}

	private void assertGraph(JFreeChart chart, String file) throws IOException
	{
		assertGraph(chart, file, !new File("resources/test/" + file).exists());
	}

	private void replaceFonts(JFreeChart chart)
	{
		int i=0;
		while (chart.getLegend(i)!=null)
		{
			chart.getLegend(i).setItemFont(font.deriveFont(chart.getLegend(i).getItemFont().getStyle(), chart.getLegend(i).getItemFont().getSize()));
			i++;
		}
		i=0;
		while (chart.getCategoryPlot().getDomainAxis(i)!=null)
		{
			chart.getCategoryPlot().getDomainAxis(i).setTickLabelFont(font.deriveFont(chart.getCategoryPlot().getDomainAxis(i).getTickLabelFont().getStyle(), chart.getCategoryPlot().getDomainAxis(i).getTickLabelFont().getSize()));
			i++;
		}
		i=0;
		while (chart.getCategoryPlot().getRangeAxis(i)!=null)
		{
			chart.getCategoryPlot().getRangeAxis(i).setTickLabelFont(font.deriveFont(chart.getCategoryPlot().getRangeAxis(i).getTickLabelFont().getStyle(), chart.getCategoryPlot().getRangeAxis(i).getTickLabelFont().getSize()));
			chart.getCategoryPlot().getRangeAxis(i).setLabelFont(font.deriveFont(chart.getCategoryPlot().getRangeAxis(i).getLabelFont().getStyle(), chart.getCategoryPlot().getRangeAxis(i).getLabelFont().getSize()));
			i++;
		}
	}

	/**
	 * Saves a JFreeChart as an SVG String. Note that the SVG may not
	 * render perfectly due to bounds issues, but it should be more
	 * consistent than PNG for unit testing.
	 *
	 * http://dolf.trieschnigg.nl/jfreechart/
	 *
	 * @param chart JFreeChart to export
	 * @param width the width of the viewport
	 * @param height the height of the viewport
	 * @return a string with SVG
	 * @throws IOException if writing the svgFile fails.
	 */
	private String saveChartAsSVG(JFreeChart chart, int width, int height) throws IOException {
		// Get a DOMImplementation and create an XML document
		DOMImplementation domImpl =
				GenericDOMImplementation.getDOMImplementation();
		Document document = domImpl.createDocument(null, "svg", null);

		// Create an instance of the SVG Generator
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

		// draw the chart in the SVG generator
		chart.draw(svgGenerator, new Rectangle(width, height));

		StringWriter stringWriter = new StringWriter();
		svgGenerator.stream(stringWriter, true /* use css */);
		return stringWriter.toString();
	}

}
