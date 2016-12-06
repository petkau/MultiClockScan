/** 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;

import java.awt.*;
import java.awt.geom.Rectangle2D.Double;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Vector;

import ij.plugin.filter.Analyzer;
import ij.plugin.frame.*;

/**
 * This Plugin is used to measure the intensity of the brightness of the image
 * of the radial scan profiles in the region of interest (ROI).
 * 
 * Reference:
 * <p>
 * "Clock-scan" protocol for image analysis Maxim Dobretsov, Dmitry Romanovsky
 * American Journal of Physiology - Cell Physiology Published 11 October 2006
 * Vol. 291 no. 5, C869-C879 DOI: 10.1152/ajpcell.00182.2006
 * 
 * @author Eugen Petkau
 */

public class Multi_Clock_Scan implements PlugIn {
	private ImagePlus imp, imp2;
	public ImageProcessor ip;

	private double limits = 1.2;
	boolean canceled = false;
	private int anzahlcells = 0;
	private Double rct;
	private double X0;
	private double Y0;
	private float min = 0;

	private FloatPolygon fp;
	private String limit = "Limits";
	private String btitle = "";
	private boolean isgrb = false;
	private boolean subbackground = false;
	private boolean paction = false;
	private boolean nslice = false;
	private boolean plotcorr = false;
	private boolean plotcorrlauf = false;
	private boolean colcorrelation = false;
	private boolean calslice = false;
	private boolean amountrois = false;
	private int currentSlice =1;
	private int anzahlslice = 0;
	private float[][] Accumulatorbild;
	private float[] Accumulatory;
	private float[] Accumulatorx;
	private float[] Accumulatorr;
	private float[] Accumulatorg;
	private float[] Accumulatorb;

	static boolean useCalibration = false;

	public void run(String arg) {
		if (IJ.versionLessThan("1.49v"))return;
		ImagePlus imp = IJ.getImage();
		if (imp == null) {
			IJ.noImage();			return;
		}
		//
		btitle = imp.getTitle();
		if (imp.getType() == 4) {
			isgrb = true;
		}
		int a[] = new int[5];
		a = imp.getDimensions();
		//IJ.log("Anzahl slices " + a[3]);
		if (a[3] > 1) {
			anzahlslice = a[3];
			nslice = true;

		}
		if(anzahlslice>1){
		 currentSlice = imp.getCurrentSlice();
		}

		RoiManager rm = RoiManager.getInstance2();
		OverlayCommands ok = new OverlayCommands();
		ok.run("remove");
		if (rm == null) {
			rm = new RoiManager();
		}
		Roi[] rois = rm.getRoisAsArray();
		if (rois.length == 0) {
				IJ.error("Multi Clock Scan","ROI Manager is empty. ROI is REquired");
			//"Multi Clock Scan","ROI Manager is empty","No ROIs Obtained"
			return;
		}
		
		

		Overlay overlay = imp.getOverlay();

		if (overlay != null) {
			overlay.clear();
		}

		int bproi = rm.getCount();
		//IJ.log("anzahl von ROI " +bproi);
		String s = "";
		if (bproi > 0)
			for (int i = 0; i < bproi; i++) 
			{    String label="";
				// int sa=rm.getSliceNumber(rois[i],label);
				 s = rois[i].getName();
				// IJ.log("Names  von ROIs " +s);
				 //int slice = rm.getSliceNumber(label);
			}
		
		if (bproi==1){
			if(anzahlslice>1){
				Roi roi = imp.getRoi();
				for (int i=1;i<=anzahlslice; i++){
					if (i==currentSlice){
						
					}else{
					imp.setSlice(i);
					rm.addRoi(roi) ;
					}
					//int slice = 
				}
				
			}
		}


		/*
		 * if (rois.length > 0){ //int i = 0; i < overlay.size(); i++ for (int
		 * i=0; i< rois.lengt.h; i++){ //String s = rois[i].getName();
		 * //rm.runCommand("Rename", "A"+i); //IJ.error("ROI Manager namen : "
		 * +s); //rename(rois[i].getName()); } }
		 */
		rm.moveRoisToOverlay(imp);

		overlay = imp.getOverlay();
		anzahlcells = overlay.size();
		if (anzahlcells > 2) {
			amountrois = true;
		}

		ip = imp.getProcessor();

		doDialog();

		if (!canceled) {
			// IJ.run("Measure");
			// rm.run("Measure");

			doMultiClockScan(ip, overlay);
			String cmd = "Measure";

			//rm.runCommand(imp, "Deselect");

			//rm.runCommand(imp, cmd);

		}
		//overlay.
		WindowManager.setTempCurrentImage(imp);

	}

	private void doMultiClockScan(ImageProcessor ip, Overlay overlay) {

		int cells = overlay.size();
	//	ArrayList xbilder = new ArrayList();//
		// Vector vec = new Vector();
		int nBins = (int) (100 * limits);
		// float[][][] Accubild = new float [cells][fp.npoints][nBins];//

		float[][] Accu = new float[cells][nBins];//
		float[][] AccuR = new float[cells][nBins];//
		float[][] AccuG = new float[cells][nBins];//
		float[][] AccuB = new float[cells][nBins];//
		RoiManager rm = RoiManager.getInstance2();

		for (int i = 0; i < overlay.size(); i++) {
			Roi roi = overlay.get(i);

			rm.select(i);

			doClockScan(ip, roi);
			// Accubild[i] =Accumulatorbild; //n picture
			Accu[i] = Accumulatory;
			AccuR[i] = Accumulatorr;
			AccuG[i] = Accumulatorg;
			AccuB[i] = Accumulatorb;
			// vec.add(Accumulatorbild);
			// abiler[i]=Accumulatorbild;

		}
		// mittlere bild

		/* vec.elementAt(i) */
		// HashSet<String> a = new HashSet<String>();
		// a.add("Julia");
		// a.add("John");
		// int len =a.size();
		// for(String one: a){
		// System.out.println(one);
		// }

		/*
		 * for (int i=0;i<cells; i++){ ip = ip.createProcessor(fp.npoints,
		 * nBins);
		 * 
		 * //ip.setFloatArray(vec.elementAt(i)); imp2 = new ImagePlus(btitle +
		 * " linear transform", ip); imp2.show(); }
		 */

		// average
		float[][] accusum = new float[2][nBins];//
		float[][] accusumR = new float[2][nBins];//
		float[][] accusumG = new float[2][nBins];//
		float[][] accusumB = new float[2][nBins];//
		// accusum [0] -average
		// accusum [1] -std
		for (int i = 0; i < nBins; i++) {
			for (int j = 0; j < cells; j++) {
				accusum[0][i] += Accu[j][i] / cells;
				accusumR[0][i] += AccuR[j][i] / cells;
				accusumG[0][i] += AccuG[j][i] / cells;
				accusumB[0][i] += AccuB[j][i] / cells;
			}

		}
		// varianz
		for (int i = 0; i < nBins; i++) {
			for (int j = 0; j < cells; j++) {
				accusum[1][i] += ((accusum[0][i] - Accu[j][i]) * (accusum[0][i] - Accu[j][i]))
						/ cells;
				accusumR[1][i] += ((accusumR[0][i] - AccuR[j][i]) * (accusumR[0][i] - AccuR[j][i]))
						/ cells;
				accusumG[1][i] += ((accusumG[0][i] - AccuG[j][i]) * (accusumG[0][i] - AccuG[j][i]))
						/ cells;
				accusumB[1][i] += ((accusumB[0][i] - AccuB[j][i]) * (accusumB[0][i] - AccuB[j][i]))
						/ cells;
			}

		}
		// standard geviation
		for (int i = 0; i < nBins; i++) {
			if (cells > 1) {
				accusum[1][i] = (float) Math.sqrt((cells / (cells - 1.0))
						* accusum[1][i]);
				accusumR[1][i] = (float) Math.sqrt((cells / (cells - 1.0))
						* accusumR[1][i]);
				accusumG[1][i] = (float) Math.sqrt((cells / (cells - 1.0))
						* accusumG[1][i]);
				accusumB[1][i] = (float) Math.sqrt((cells / (cells - 1.0))
						* accusumB[1][i]);
			} else {

				accusum[1][i] = (float) Math.sqrt(accusum[1][i]);
				accusumR[1][i] = (float) Math.sqrt(accusumR[1][i]);
				accusumG[1][i] = (float) Math.sqrt(accusumG[1][i]);
				accusumB[1][i] = (float) Math.sqrt(accusumB[1][i]);
			}

		}

		/*
		 * 
		 * if (cells > 0) {
		 * 
		 * Plot plot = null; plot = new Plot("Clock Scan Profile Plot",
		 * "scan length, % of radius ", "intensity, shades of grey",
		 * Accumulatorx, accusum[0]); //plot.addPoints(Accumulatorx,
		 * accusumR[0], shape); //plot.addErrorBars(accusum[1]); //nur wenn
		 * motwendig //plot.a
		 * 
		 * plot.show(); }
		 */
		//
		ResultsTable rt = Analyzer.getResultsTable();
		if (rt == null) {
		        rt = new ResultsTable();
		        Analyzer.setResultsTable(rt);
		}

		if (cells >= 0) {
			// averaged from all calls
			Plot plot = null;

			plot = new Plot("Multi Clock Scan averaged Profile Plot of " + cells
					+ " ROIs " + btitle, "scan length, % of radius ",
					"intensity, shades of grey");

			plot.setSize(600, 350);
			plot.setLineWidth(2);
			plot.addPoints(Accumulatorx, accusum[0], PlotWindow.LINE);
			plot.setLineWidth(1);
			if (paction) {
				plot.setXYLabels("scan length, % of radius ",
						"intensity, shades of grey with StdDev");
				plot.setColor(Color.black);
				plot.setLineWidth(5);
				plot.addErrorBars(accusum[1]); // nur wenn motwendig
				plot.setColor(Color.black);
				plot.setLineWidth(1);
			}
			plot.setColor(Color.black);
			if (isgrb) {
				plot.setColor(Color.red);
				plot.addPoints(Accumulatorx, accusumR[0], PlotWindow.LINE);
				if (paction) {
					plot.addErrorBars(accusumR[1]);
				}
				plot.setColor(Color.green);
				plot.addPoints(Accumulatorx, accusumG[0], PlotWindow.LINE);
				if (paction) {

					plot.addErrorBars( accusumG[1]); // nur wenn motwendig
				}
				plot.setColor(Color.blue);
				plot.addPoints(Accumulatorx, accusumB[0], PlotWindow.LINE);
				if (paction) {
	
					plot.addErrorBars( accusumB[1]); // nur wenn motwendig
				}
				plot.setColor(Color.black);
				// labels
				if(isgrb) {
				plot.setJustification(Plot.RIGHT);
				plot.setLineWidth(2);
				double xloc = 0.2;
				double yloc = 0.07;
				plot.setColor(Color.black);
				
				plot.addLabel(xloc, yloc, "Y0   - RGB");
				plot.addLabel(xloc, yloc + 0.04, "Y1  -   Red");
				plot.addLabel(xloc, yloc + 0.08, "Y2 - Green");
				plot.addLabel(xloc, yloc + 0.12, "Y3   -  Blue");

				xloc += 0.01;
				yloc -= 0.01;
				plot.setColor(Color.black);
				plot.drawNormalizedLine(xloc, yloc, xloc + 0.1, yloc);
				plot.setColor(Color.red);
				plot.drawNormalizedLine(xloc, yloc + 0.04, xloc + 0.1,
						yloc + 0.04);
				plot.setColor(Color.green);
				plot.drawNormalizedLine(xloc, yloc + 0.08, xloc + 0.1,
						yloc + 0.08);
				plot.setColor(Color.blue);
				plot.drawNormalizedLine(xloc, yloc + 0.12, xloc + 0.1,
						yloc + 0.12);
			}
				plot.setLimitsToFit(true);
			}

			plot.show();
			//plot.getResultsTable();
			
           //rt.show("");
		}


		// IJ.log("  clo heading " +rt.getColumnHeadings());
		// IJ.log(" anzahl zels  " +cells);
		// das ist plott von kurven

		// all cells
		if (cells >= 2) {
			Plot plot = null;
			plot = new Plot("Multi Clock Scan Profile Plot of " + cells + " ROIs "
					+ btitle, "scan length, % of radius ",
					"intensity, shades of grey");
			plot.setSize(600, 350);
			// plot.setLineWidth(2);
			// plot.addPoints(Accumulatorx, Accu[0], PlotWindow.LINE);
			plot.setLineWidth(1);
			for (int i = 0; i < cells; i++) {
				plot.setLineWidth(2);
				plot.addPoints(Accumulatorx, Accu[i], PlotWindow.LINE);
			}
			plot.setLineWidth(1);

			if (isgrb) {
				plot.setColor(Color.red);
				for (int i = 0; i < cells; i++) {
					plot.addPoints(Accumulatorx, AccuR[i], PlotWindow.LINE);
				}
				plot.setColor(Color.green);
				for (int i = 0; i < cells; i++) {
					plot.addPoints(Accumulatorx, AccuG[i], PlotWindow.LINE);
				}
				plot.setColor(Color.blue);
				for (int i = 0; i < cells; i++) {
					plot.addPoints(Accumulatorx, AccuB[i], PlotWindow.LINE);
				}
				plot.setColor(Color.black);
			}

			// labels für rgb
			if (isgrb) {
			plot.setJustification(Plot.RIGHT);
			plot.setLineWidth(2);
			double xloc = 0.2;
			double yloc = 0.07;
			plot.setColor(Color.black);
			
			
			plot.addLabel(xloc, yloc, "Y0-Y"+(cells -1)+"...- RGB");
			plot.addLabel(xloc, yloc + 0.04, "Y"+cells+"-Y"+(2*cells -1)+"...-  Red");
			plot.addLabel(xloc, yloc + 0.08, "Y"+2*cells+"-Y"+(3*cells -1)+".- Green");
			plot.addLabel(xloc, yloc + 0.12, "Y"+3*cells+"-Y"+(4*cells -1)+"..-  Blue");

			xloc += 0.01;
			yloc -= 0.01;
			plot.setColor(Color.black);
			plot.drawNormalizedLine(xloc, yloc, xloc + 0.1, yloc);
			plot.setColor(Color.red);
			plot.drawNormalizedLine(xloc, yloc + 0.04, xloc + 0.1,
					yloc + 0.04);
			plot.setColor(Color.green);
			plot.drawNormalizedLine(xloc, yloc + 0.08, xloc + 0.1,
					yloc + 0.08);
			plot.setColor(Color.blue);
			plot.drawNormalizedLine(xloc, yloc + 0.12, xloc + 0.1,
					yloc + 0.12);
			}//end labela rgb
			
			plot.setLimitsToFit(true);
			plot.show();
		}
		int lineofcorrelation = 30;
		int lineofendcorr = 15;

		float[] resaltcorRG = new float[accusumR[0].length - lineofcorrelation];
		float[] resaltcorRB = new float[accusumR[0].length - lineofcorrelation];
		float[] resaltcorGB = new float[accusumR[0].length - lineofcorrelation];
		int assaa = accusumR[0].length;

		// IJ.log("correlate length = "+assaa );
		for (int i = 0; i < (accusumR[0].length - lineofcorrelation); i++) {
			// Arrays.copyOfRange (accusumR[0], i+14,i+34);

			resaltcorRG[i] = pearson(
					Arrays.copyOfRange(accusumR[0], i + lineofendcorr - 1, i
							+ lineofcorrelation - 1),
					Arrays.copyOfRange(accusumG[0], i + lineofendcorr - 1, i
							+ lineofcorrelation - 1));
			resaltcorRB[i] = pearson(
					Arrays.copyOfRange(accusumR[0], i + lineofendcorr - 1, i
							+ lineofcorrelation - 1),
					Arrays.copyOfRange(accusumB[0], i + lineofendcorr - 1, i
							+ lineofcorrelation - 1));
			resaltcorGB[i] = pearson(
					Arrays.copyOfRange(accusumG[0], i + lineofendcorr - 1, i
							+ lineofcorrelation - 1),
					Arrays.copyOfRange(accusumB[0], i + lineofendcorr - 1, i
							+ lineofcorrelation - 1));

		}
		int assa = resaltcorRG.length;

		float Axcex[] = Arrays.copyOfRange(Accumulatorx, lineofendcorr,
				Accumulatorx.length - lineofendcorr);

		// IJ.log("correlate length = "+assa );

		if (isgrb) {
			float rgcorrelation = pearson(accusumR[0], accusumG[0]);
			float rbcorrelation = pearson(accusumR[0], accusumB[0]);
			float gbcorrelation = pearson(accusumG[0], accusumB[0]);

			float rgcorrelation100 = pearson100(accusumR[0], accusumG[0]);
			float rbcorrelation100 = pearson100(accusumR[0], accusumB[0]);
			float gbcorrelation100 = pearson100(accusumG[0], accusumB[0]);
			// ////////////////////////
			// /correlation accusumR[0] accusumG[0] accusumG[0]
			// PearsonsCorrelation pcoool = new PearsonsCorrelation();
			// Plot plot = null;

			float[] festcorrRG = new float[assa];
			Arrays.fill(festcorrRG, rgcorrelation100);
			float[] festcorrRB = new float[assa];
			Arrays.fill(festcorrRB, rbcorrelation100);
			float[] festcorrGB = new float[assa];
			Arrays.fill(festcorrGB, gbcorrelation100);

			plotcorrlauf = true;
			if (colcorrelation) {
				Plot plot = null;
				// plot.setLimits(0.1, n/10, 0.1, n/10);
				// plot.setLimits(lineofendcorr, assa-lineofendcorr, -2, +2);
				// verteilung von correlation
				// pearson correlation coefficient
				// scan length, % of radius

				plot = new Plot("Multi Clock Scan Correlation Plot " + btitle,
						"X Axis  ", "Y Axis ");
				plot.setXYLabels("scan length, % of radius ",
						"Pearson correlation coefficient in ROIs");
				plot.setSize(433, 400);
				plot.setLineWidth(2);
				plot.setLimits(lineofendcorr, 100, -2, +2);
				//
				
				// plot.setLegend(labels, flags)
				plot.setColor(Color.YELLOW);
				plot.addPoints(Axcex, resaltcorRG, 0);
				plot.setLineWidth(2);
				plot.addPoints(Axcex, festcorrRG, Plot.LINE);

				plot.setColor(Color.MAGENTA);
				plot.addPoints(Axcex, resaltcorRB, 4);
				plot.setLineWidth(2);
				plot.addPoints(Axcex, festcorrRB, Plot.LINE);

				plot.setColor(Color.CYAN);
				plot.addPoints(Axcex, resaltcorGB, 3);
				plot.setLineWidth(2);
				plot.addPoints(Axcex, festcorrGB, Plot.LINE);

				// Labels
				plot.setJustification(Plot.RIGHT);
				double xloc = 0.2;
				double yloc = 0.07;
				plot.setColor(Color.black);
				plot.addLabel(xloc, yloc, "Red Green ");
				plot.addLabel(xloc, yloc + 0.04, "Red Blue ");
				plot.addLabel(xloc, yloc + 0.08, "Green Blue ");
				xloc += 0.01;
				yloc -= 0.01;
				plot.setColor(Color.YELLOW);
				plot.drawNormalizedLine(xloc, yloc, xloc + 0.1, yloc);
				plot.setColor(Color.MAGENTA);
				plot.drawNormalizedLine(xloc, yloc + 0.04, xloc + 0.1,
						yloc + 0.04);
				plot.setColor(Color.CYAN);
				plot.drawNormalizedLine(xloc, yloc + 0.08, xloc + 0.1,
						yloc + 0.08);
				// plot.add("CIRCLE",xloc, yloc+0.12);
				// plot.addLabel(xloc, yloc+0.12, "BLEOUR");

				plot.setLimitsToFit(true);
				plot.show();

			}

			// plotcorr = true;
			if (colcorrelation) {
				Plot plot = null;
				// titel ???? verteilung R-G R-B G-B
				plot = new Plot("Multi Clock Scan Intensity, shades of RGB  Plot "
						+ btitle, "X Axis   ", "Y Axis  ");// ,
															// accusumR[0],
				plot.setXYLabels("intensity, shades of RGB (channels) in ROIs",
						"intensity, shades of RGB (channels) in ROIs");
				// accusumG[0]);
				// plot.setLimits(0, 256, 0, 256);
				plot.setSize(433, 400);
				plot.setLineWidth(2);
				// plot.setLegend(labels, flags)
				plot.setColor(Color.YELLOW);
				plot.addPoints(accusumG[0], accusumR[0], 0);

				plot.setColor(Color.MAGENTA);
				plot.addPoints(accusumB[0], accusumR[0], 4);

				plot.setColor(Color.CYAN);
				plot.addPoints(accusumB[0], accusumG[0], 3);

				// Labels
				plot.setJustification(Plot.RIGHT);
				double xloc = 0.2;
				double yloc = 0.07;
				plot.setColor(Color.black);
				plot.addLabel(xloc, yloc, "Red Green ");
				plot.addLabel(xloc, yloc + 0.04, "Red Blue ");
				plot.addLabel(xloc, yloc + 0.08, "Green Blue ");
				xloc += 0.01;
				yloc -= 0.01;
				plot.setColor(Color.YELLOW);
				plot.drawNormalizedLine(xloc, yloc, xloc + 0.1, yloc);
				plot.setColor(Color.MAGENTA);
				plot.drawNormalizedLine(xloc, yloc + 0.04, xloc + 0.1,
						yloc + 0.04);
				plot.setColor(Color.CYAN);
				plot.drawNormalizedLine(xloc, yloc + 0.08, xloc + 0.1,
						yloc + 0.08);

				plot.setLimitsToFit(true);
				plot.show();
			}

			// correlation
			// IJ.log(" correlation in ROIs with limit R-G " + rgcorrelation
			// + " correlation R-B " + rbcorrelation + " correlation G-B "
			// + gbcorrelation);
			// if (colcorrelation){
			//IJ.log(" correlation in the ROIs :  R-G " + rgcorrelation100
				//	+ " : R-B " + rbcorrelation100 + " : G-B "
				//	+ gbcorrelation100);
			// }
			// plot.show();
		}

	}

	// public static HashSet<float[][] > zz = new HashSet<float[][] > () ;

	// zz.size()
	/*
	 * int i =0; for( float[][] one_arr : zz){ i++;
	 */

	private void doClockScan(ImageProcessor ip, Roi roi) {
		imp = IJ.getImage();
		roi = imp.getRoi();
		RoiManager manager = RoiManager.getInstance();
		if (manager == null)
			manager = new RoiManager();

		if (roi != null) {

			if (roi.getName() != "interpolate") {
				interpolate();
				roi = imp.getRoi();

			}
		}

		int nBins = (int) (100 * limits); // realradius was 100

		setXYcenter();
		if (roi != null) {
			scaletocenter(roi, limits, limits, X0, Y0);
			Roi roi3 = imp.getRoi();
			roi3.setName(limit + " : " + limits);

		}

		fp = imp.getRoi().getFloatPolygon();

		Accumulatory = new float[nBins];
		Accumulatorx = new float[nBins];
		Accumulatorr = new float[nBins];
		Accumulatorg = new float[nBins];
		Accumulatorb = new float[nBins];
		Accumulatorbild = new float[fp.npoints][nBins];
		float[][] AccumulatorRGB = new float[3][nBins];
		float[][] Accpixel = new float[fp.npoints][nBins];

		float[][] Accumulator = new float[2][nBins];
		for (int i = 0; i < fp.npoints; i++) {

			double dx = (fp.xpoints[i] - X0);
			double dy = (fp.ypoints[i] - Y0);
			double radius = Math.sqrt(dx * dx + dy * dy);
			double sinA = dy / radius;
			double cosA = dx / radius;
			for (int j = 0; j < nBins; j++) {
				double newX = X0 + cosA * j * radius / nBins;
				double newY = Y0 + sinA * j * radius / nBins;
				// int[] rgb = imp.getPixel((int) newX, (int) newY);// new
				Accpixel[i][j] = ip.getPixel((int) newX, (int) newY);
				Accumulatorbild[i][j] = ip.getPixel((int) newX, (int) newY);
				Accumulator[0][j] = Accumulator[0][j] + 1;//
				Accumulator[1][j] = Accumulator[1][j]
						+ ip.getPixelValue((int) newX, (int) newY); //
				// Accumulator[2][j] = Accumulator[2][j] + imp.getPixel((int)
				// newX, (int) newY); //
				AccumulatorRGB[0][j] = AccumulatorRGB[0][j]
						+ imp.getPixel((int) newX, (int) newY)[0];
				AccumulatorRGB[1][j] = AccumulatorRGB[1][j]
						+ imp.getPixel((int) newX, (int) newY)[1];
				AccumulatorRGB[2][j] = AccumulatorRGB[2][j]
						+ imp.getPixel((int) newX, (int) newY)[2];

			}
		}
		// zz.add(Accumulatorbild);
		Calibration cal = imp.getCalibration();
		cal.setUnit("pixel");
		if (cal.getUnit() == "pixel")
			useCalibration = false;
		// Plot plot = null;
		if (useCalibration) {
			for (int i = 0; i < nBins; i++) {
				Accumulator[1][i] = Accumulator[1][i] / Accumulator[0][i];
				Accumulator[0][i] = (float) (cal.pixelWidth * nBins * ((double) (i + 1) / nBins));
			}

		} else {

			min = 0;
			for (int i = 0; i < nBins; i++) {
				Accumulator[1][i] = Accumulator[1][i] / Accumulator[0][i]; // werte
				AccumulatorRGB[0][i] = AccumulatorRGB[0][i] / Accumulator[0][i]; // werte
				AccumulatorRGB[1][i] = AccumulatorRGB[1][i] / Accumulator[0][i]; // werte
				AccumulatorRGB[2][i] = AccumulatorRGB[2][i] / Accumulator[0][i]; // werte
				Accumulator[0][i] = (float) (nBins * ((double) (i + 1) / nBins));// anzahl
																					// points

				Accumulatorx[i] = Accumulator[0][i];
				Accumulatory[i] = Accumulator[1][i]; // werte
				Accumulatorr[i] = AccumulatorRGB[0][i];
				Accumulatorg[i] = AccumulatorRGB[1][i];
				Accumulatorb[i] = AccumulatorRGB[2][i];

				if (Accumulator[1][i] > min) {
					min = Accumulator[1][i];
				}

			}
			// subbackground
			float[][] Accusubbackground = new float[1][nBins];

			for (int j = 0; j < nBins; j++) {
				Accusubbackground[0][j] = Accumulator[1][j] - min;// -Accumulator[1][1];
				Accumulator[0][j] = Accumulator[0][j];

			}

		}

	}

	private void doDialog() {
		canceled = false;
		GenericDialog gd = new GenericDialog("Multi Clock Scan...",
				IJ.getInstance());
		gd.addMessage("scan limits (fraction of radius)\n");
		gd.addSlider("value", 1, 2, 1.2);
		// gd.addCheckbox("subtract background", subbackground);
		if (amountrois) {
			gd.addCheckbox("Plot with standard deviation ", paction);
		}
		gd.addMessage("Number of ROIs: " + anzahlcells);

		if (isgrb) {
			gd.addMessage("You have RGB Image ");
			//gd.addCheckbox("calculate correlation of color", colcorrelation);
		}
		if (nslice) {
			// You have stack of ## images.
			gd.addMessage("You have stack of " + anzahlslice + " images.");
			//gd.addMessage("You current Slice"   +currentSlice);
			gd.addMessage("All images will be analyzed");
			//gd.addMessage("All images will be analyzed", calslice);
		}
		
		String textcit = "If you use this plugin for your research,\n please cite the original article: \n Dobretsov & Romanovsky, \n Clock-scan protocol  for image analysis.AJP,\n Cell Physiology 291: C869-C879, 2006";
		gd.addMessage("");

		gd.addMessage(textcit);

		gd.showDialog();
		if (gd.wasCanceled()) {
			canceled = true;
			return;
		}

		limits = gd.getNextNumber();
		// subbackground = gd.getNextBoolean();
		if (amountrois) {
			paction = gd.getNextBoolean();
		}

		if (isgrb) {
			//colcorrelation = gd.getNextBoolean();
		}
		/*
		if (calslice) {
			calslice = gd.getNextBoolean();

		}
		*/

		if (gd.invalidNumber()) {
			IJ.showMessage("Error", "Invalid input Number");
			canceled = true;
			return;
		}
	}

	private void setXYcenter() {

		rct = imp.getRoi().getFloatBounds();
		X0 = rct.getCenterX();
		Y0 = rct.getCenterY();

	}

	void scaletocenter(Roi roi, double xscale, double yscale, double x, double y) {

		FloatPolygon poly = roi.getFloatPolygon();
		int type = roi.getType();
		double xbase = x;
		double ybase = y;
		for (int i = 0; i < poly.npoints; i++) {

			poly.xpoints[i] = (float) ((poly.xpoints[i] - xbase) * xscale + xbase);
			poly.ypoints[i] = (float) ((poly.ypoints[i] - ybase) * yscale + ybase);

		}
		Roi roi2 = null;
		type = Roi.FREEROI;
		roi2 = new PolygonRoi(poly.xpoints, poly.ypoints, poly.npoints, type);
		roi2.setStrokeColor(roi.getStrokeColor());
		if (roi.getStroke() != null)
			roi2.setStroke(roi.getStroke());
		imp.setRoi(roi2);
	}

	public void interpolate() {
		Roi roi = imp.getRoi();
		if (roi == null) {
			return;
		}
		if (roi.getType() == Roi.POINT)
			return;

		double interval = 1;
		boolean smooth = true;
		Undo.setup(Undo.ROI, imp);
		boolean adjust = true;
		int sign = adjust ? -1 : 1;
		FloatPolygon poly = roi.getInterpolatedPolygon(sign * interval, smooth);
		int t = roi.getType();
		int type = roi.isLine() ? Roi.FREELINE : Roi.FREEROI;
		if (t == Roi.POLYGON && interval > 1.0)
			type = Roi.POLYGON;
		if ((t == Roi.RECTANGLE || t == Roi.OVAL || t == Roi.FREEROI)
				&& interval >= 8.0)
			type = Roi.POLYGON;
		if ((t == Roi.LINE || t == Roi.FREELINE) && interval >= 8.0)
			type = Roi.POLYLINE;
		if (t == Roi.POLYLINE && interval >= 8.0)
			type = Roi.POLYLINE;
		ImageCanvas ic = imp.getCanvas();
		if (poly.npoints <= 150 && ic != null && ic.getMagnification() >= 12.0)
			type = roi.isLine() ? Roi.POLYLINE : Roi.POLYGON;
		Roi p = new PolygonRoi(poly, type);
		if (roi.getStroke() != null)
			p.setStrokeWidth(roi.getStrokeWidth());
		p.setStrokeColor(roi.getStrokeColor());
		p.setName(roi.getName());
		transferProperties(roi, p);
		imp.setRoi(p);
	}

	private void transferProperties(Roi roi1, Roi roi2) {
		if (roi1 == null || roi2 == null)
			return;
		roi2.setStrokeColor(roi1.getStrokeColor());
		if (roi1.getStroke() != null)
			roi2.setStroke(roi1.getStroke());
		roi2.setDrawOffset(roi1.getDrawOffset());
	}

	private float pearson(float[] x, float[] y) {
		if (x.length != y.length) {
			throw new RuntimeException("Length of x (" + x.length
					+ ") does not equal length of y (" + y.length + ")");
		}

		int N = 0;
		double sumX = 0, sumY = 0;
		double sumSqX = 0, sumSqY = 0;
		double sumXY = 0;
		for (int i = 0; i < x.length; i++) {
			// Skip NaN / Infinity values in the correlation calculation
			if (!Float.isNaN(x[i]) && !Float.isInfinite(x[i])
					&& !Float.isNaN(y[i]) && !Float.isInfinite(y[i])) {
				N++;
				sumX += x[i];
				sumY += y[i];
				sumSqX += x[i] * x[i];
				sumSqY += y[i] * y[i];
				sumXY += x[i] * y[i];
			}
		}

		return (float) ((N * sumXY - sumX * sumY)
				/ Math.sqrt(N * sumSqX - sumX * sumX) / Math.sqrt(N * sumSqY
				- sumY * sumY));
	}

	private float pearson100(float[] x, float[] y) {
		if (x.length != y.length) {
			throw new RuntimeException("Length of x (" + x.length
					+ ") does not equal length of y (" + y.length + ")");
		}

		int N = 0;
		double sumX = 0, sumY = 0;
		double sumSqX = 0, sumSqY = 0;
		double sumXY = 0;
		for (int i = 0; i < 100; i++) {
			// Skip NaN / Infinity values in the correlation calculation
			if (!Float.isNaN(x[i]) && !Float.isInfinite(x[i])
					&& !Float.isNaN(y[i]) && !Float.isInfinite(y[i])) {
				N++;
				sumX += x[i];
				sumY += y[i];
				sumSqX += x[i] * x[i];
				sumSqY += y[i] * y[i];
				sumXY += x[i] * y[i];
			}
		}

		return (float) ((N * sumXY - sumX * sumY)
				/ Math.sqrt(N * sumSqX - sumX * sumX) / Math.sqrt(N * sumSqY
				- sumY * sumY));
	}

}
