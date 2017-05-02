package util;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Cartesian extends JPanel {

	private int width = 800;
	private int heigth = 400;
	private int padding = 25;
	private int labelPadding = 25;
	private Color lineColor = new Color(22, 101, 130, 140);
	private Color pointColor = new Color(100, 100, 100, 180);
	private Color gridColor = new Color(200, 200, 200, 200);
	private static final Stroke GRAPH_STROKE = new BasicStroke(2f);
	private int pointWidth = 4;
	private int numberYDivisions = 25;
	private List<Pair<Double, Double>> scores;

	public Cartesian(List<Pair<String, String>> scores) {
		this.scores = new LinkedList<>();
		setPointsList(scores);
	}

	private void setPointsList(List<Pair<String, String>> list) {
		for (Pair<String, String> P : list) {

			scores.add(new Pair<Double, Double>(Double.parseDouble(P.getFirst()), Double.parseDouble(P.getSecond())));

		}

		Collections.sort(scores, new Comparator<Pair<Double, Double>>() {
			@Override
			public int compare(final Pair<Double, Double> o1, final Pair<Double, Double> o2) {
				return Double.compare(Double.parseDouble(o1.getFirst().toString()),
						Double.parseDouble(o2.getFirst().toString()));
			}
		});

//		for (Pair<Double, Double> P : scores) {
//			System.out.println(P.getFirst() + " " + P.getSecond());
//		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		double xScale = ((double) getWidth() - (2 * padding) - labelPadding) / (scores.size() - 1);
		double yScale = ((double) getHeight() - 2 * padding - labelPadding) / (getMaxScore() - getMinScore());

		List<Point> graphPoints = new ArrayList<>();
		for (int i = 0; i < scores.size(); i++) {
			int x1 = (int) (i * xScale + padding + labelPadding);
			int y1 = (int) ((getMaxScore() - scores.get(i).getSecond()) * yScale + padding);
			graphPoints.add(new Point(x1, y1));
		}

		// draw white background
		g2.setColor(Color.WHITE);
		g2.fillRect(padding + labelPadding, padding, getWidth() - (2 * padding) - labelPadding,
				getHeight() - 2 * padding - labelPadding);
		g2.setColor(Color.BLACK);

		// create hatch marks and grid lines for y axis.
		for (int i = 0; i < numberYDivisions + 1; i++) {
			int x0 = padding + labelPadding;
			int x1 = pointWidth + padding + labelPadding;
			int y0 = getHeight()
					- ((i * (getHeight() - padding * 2 - labelPadding)) / numberYDivisions + padding + labelPadding);
			int y1 = y0;
			if (scores.size() > 0) {
				g2.setColor(gridColor);
				g2.drawLine(padding + labelPadding + 1 + pointWidth, y0, getWidth() - padding, y1);
				g2.setColor(Color.BLACK);
				String yLabel = ((int) ((getMinScore()
						+ (getMaxScore() - getMinScore()) * ((i * 1.0) / numberYDivisions)) * 100)) / 100.0 + "";
				FontMetrics metrics = g2.getFontMetrics();
				int labelWidth = metrics.stringWidth(yLabel);
				g2.drawString(yLabel, x0 - labelWidth - 5, y0 + (metrics.getHeight() / 2) - 3);
			}
			g2.drawLine(x0, y0, x1, y1);
		}

		// and for x axis
		for (int i = 0; i < scores.size(); i++) {
			if (scores.size() > 1) {
				int x0 = i * (getWidth() - padding * 2 - labelPadding) / (scores.size() - 1) + padding + labelPadding;
				int x1 = x0;
				int y0 = getHeight() - padding - labelPadding;
				int y1 = y0 - pointWidth;
				if ((i % ((int) ((scores.size() / 20.0)) + 1)) == 0) {
					g2.setColor(gridColor);
					g2.drawLine(x0, getHeight() - padding - labelPadding - 1 - pointWidth, x1, padding);
					g2.setColor(Color.BLACK);

					String timestamp = scores.get(i).getFirst().toString();
					Date date = new Date((int) Double.parseDouble(timestamp) * 1000L);

					SimpleDateFormat hourFormatter = new SimpleDateFormat("HH");
					SimpleDateFormat dayFormatter = new SimpleDateFormat("dd/MM");

					String[] hour = hourFormatter.format(date).toString().split("\\:");
					String day = dayFormatter.format(date).toString();

					String xLabel = hour[0] + " -- " + day + "";
					FontMetrics metrics = g2.getFontMetrics();
					int labelWidth = metrics.stringWidth(xLabel);
					g2.drawString(xLabel, x0 - labelWidth / 2, y0 + metrics.getHeight() + 3);
				}
				g2.drawLine(x0, y0, x1, y1);
			}
		}

		// create x and y axes
		g2.drawLine(padding + labelPadding, getHeight() - padding - labelPadding, padding + labelPadding, padding);
		g2.drawLine(padding + labelPadding, getHeight() - padding - labelPadding, getWidth() - padding,
				getHeight() - padding - labelPadding);

		Stroke oldStroke = g2.getStroke();
		g2.setColor(lineColor);
		g2.setStroke(GRAPH_STROKE);
		for (int i = 0; i < graphPoints.size() - 1; i++) {
			int x1 = graphPoints.get(i).x;
			int y1 = graphPoints.get(i).y;
			int x2 = graphPoints.get(i + 1).x;
			int y2 = graphPoints.get(i + 1).y;
			g2.drawLine(x1, y1, x2, y2);
		}

		g2.setStroke(oldStroke);
		g2.setColor(pointColor);
		for (int i = 0; i < graphPoints.size(); i++) {
			int x = graphPoints.get(i).x - pointWidth / 2;
			int y = graphPoints.get(i).y - pointWidth / 2;
			int ovalW = pointWidth;
			int ovalH = pointWidth;
			g2.fillOval(x, y, ovalW, ovalH);
		}
	}

	// @Override
	// public Dimension getPreferredSize() {
	// return new Dimension(width, heigth);
	// }
	private double getMinScore() {
		double minScore = Double.MAX_VALUE;
		for (Pair<Double, Double> score : scores) {
			minScore = Math.min(minScore, score.getSecond());
		}
		return minScore;
	}

	private double getMaxScore() {
		double maxScore = Double.MIN_VALUE;
		for (Pair<Double, Double> score : scores) {
			maxScore = Math.max(maxScore, score.getSecond());
		}
		return maxScore;
	}

	public void setScores(List<Pair<Double, Double>> scores) {
		this.scores = scores;
		invalidate();
		this.repaint();
	}

	public List<Pair<Double, Double>> getScores() {
		return scores;
	}

	private static void createAndShowGui(List<Pair<String, String>> scores) {
		MainPanel mainPanel = new MainPanel(scores);
		mainPanel.setPreferredSize(new Dimension(800, 600));
		JFrame frame = new JFrame("DrawGraph");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(mainPanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public static void createPlane(List<Pair<String, String>> scores) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGui(scores);
			}
		});
	}

	// Main changes underneath

	static class MainPanel extends JPanel {

		public MainPanel(List<Pair<String, String>> scores) {

			setLayout(new BorderLayout());

			JLabel title = new JLabel("Variation of temperature with time");
			title.setFont(new Font("Arial", Font.BOLD, 25));
			title.setHorizontalAlignment(JLabel.CENTER);

			JPanel graphPanel = new Cartesian(scores);

			VerticalPanel vertPanel = new VerticalPanel();

			HorizontalPanel horiPanel = new HorizontalPanel();

			add(title, BorderLayout.NORTH);
			add(horiPanel, BorderLayout.SOUTH);
			add(vertPanel, BorderLayout.WEST);
			add(graphPanel, BorderLayout.CENTER);

		}

		class VerticalPanel extends JPanel {

			public VerticalPanel() {
				setPreferredSize(new Dimension(25, 0));
			}

			@Override
			public void paintComponent(Graphics g) {

				super.paintComponent(g);

				Graphics2D gg = (Graphics2D) g;
				gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				Font font = new Font("Arial", Font.PLAIN, 15);

				String string = "Temperature (�C)";

				FontMetrics metrics = g.getFontMetrics(font);
				int width = metrics.stringWidth(string);
				int height = metrics.getHeight();

				gg.setFont(font);

				drawRotate(gg, getWidth(), (getHeight() + width) / 2, 270, string);
			}

			public void drawRotate(Graphics2D gg, double x, double y, int angle, String text) {
				gg.translate((float) x, (float) y);
				gg.rotate(Math.toRadians(angle));
				gg.drawString(text, 0, 0);
				gg.rotate(-Math.toRadians(angle));
				gg.translate(-(float) x, -(float) y);
			}

		}

		class HorizontalPanel extends JPanel {

			public HorizontalPanel() {
				setPreferredSize(new Dimension(0, 25));
			}

			@Override
			public void paintComponent(Graphics g) {

				super.paintComponent(g);

				Graphics2D gg = (Graphics2D) g;
				gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				Font font = new Font("Arial", Font.PLAIN, 15);

				String string = "Time (s)";

				FontMetrics metrics = g.getFontMetrics(font);
				int width = metrics.stringWidth(string);
				int height = metrics.getHeight();

				gg.setFont(font);

				gg.drawString(string, (getWidth() - width) / 2, 11);
			}

		}

	}

}