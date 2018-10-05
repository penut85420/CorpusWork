package edu.ntou.cs.nlp.wikipedia;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.oppai.io.LibraryIO;
import org.oppai.utils.LibraryUtils;
import org.w3c.dom.*;

import edu.ntou.cs.nlp.wordSegmentation.ui.SegmentorApp;
import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.*;

public class CategoryApp extends Application {

	static Label t1 = new Label("File:  ");
	static Label t2 = new Label("Title TW:  ");
	static Label t3 = new Label("Title CN:  ");
	static CheckBox isErrContent = new CheckBox("First Paragraph (Check if error)");
	static Label tRecordTotal = new Label("Total Record: 0");

	static TextField tFile = new TextField();
	static TextField tTitleTW = new TextField();
	static TextField tTitleCN = new TextField();
	static TextArea tContent = new TextArea();

	static Button bCopy = new Button("Copy");
	static Button bNext = new Button("Next");
	static Button bPre = new Button("Previous");
	static Button bRnd = new Button("Random");

	static ArrayList<CheckBox> categoryList;

	static FlowPane pCategory = new FlowPane(Orientation.VERTICAL);

	static File[] fileList;
	static Document currentDoc;
	static Document docErr;
	static Document docRecord;
	static NodeList titleList;
	static int fileID;
	static int pageID;

	final static String dirInn = "D:\\Documents\\Corpus\\Wiki\\TitleList\\";
	final static String dirOut = "D:\\Documents\\Corpus\\Wiki\\TitleList_Marked\\";
	final static String errFilePath = "err.xml";
	final static String recordFilePath = "record.xml";

	@Override
	public void start(Stage primaryStage) throws Exception {
		initGUI(primaryStage);
		initFout();
		initDisplay();
		display();

		bNext.setOnAction((e) -> { actionOnPage(1); });
		bPre.setOnAction((e) -> { actionOnPage(-1); });
		bRnd.setOnAction((e) -> { rndDisplay(); });
		bCopy.setOnAction((e) -> {
			ClipboardContent cc = new ClipboardContent();
			cc.putString("https://zh.wikipedia.org/wiki/" + tTitleTW.getText());
			Clipboard.getSystemClipboard().setContent(cc);
		});
	}

	public static void actionOnPage(int n) {
		save();
		pageID += n;
		chkNumber();
		display();
	}

	public static void save() {
		// Need to save on original xml file

		// Set attr "marked" to 1 at category tag
		Element page = (Element) currentDoc.getElementsByTagName("title").item(pageID);
		Element category = (Element) page.getElementsByTagName("category").item(0);
		NodeList cat = page.getElementsByTagName("cat");

		category.setAttribute("marked", "1");

		// Set attr "isa" to cat tag
		for (int i = 0; i < cat.getLength(); i++)
			((Element) cat.item(i)).setAttribute("isa", categoryList.get(i).isSelected() ? "1" : "0");
		
		Node clone_page = page.cloneNode(true);
		Node n = docRecord.importNode(clone_page, true);
		
		// If the content has some errors, save to err.xml. Otherwise, save to record.xml.
		if (isErrContent.isSelected()) {
			n = docErr.importNode(clone_page, true);
			docErr.getFirstChild().appendChild(n);
		} else docRecord.getFirstChild().appendChild(n); 

		// Save file
		try { LibraryIO.writeXML(fileList[fileID].getPath(), currentDoc); } 
		catch (Exception e) { e.printStackTrace(); }
		try { LibraryIO.writeXML(dirOut + errFilePath, docErr); }
		catch (Exception e) { e.printStackTrace(); }
		try { LibraryIO.writeXML(dirOut + recordFilePath, docRecord); }
		catch (Exception e) { e.printStackTrace(); }
	}

	public static void chkNumber() {
		if (pageID == titleList.getLength()) {
			fileID++;
			try { currentDoc = LibraryIO.loadXML(fileList[fileID]); } 
			catch (Exception ee) { ee.printStackTrace(); }
			pageID = 0;
		}
	}

	public static void rndDisplay() {
		save();
		fileID = LibraryUtils.getRand(0, fileList.length);
		try { currentDoc = LibraryIO.loadXML(fileList[fileID]); } 
		catch (Exception ee) { ee.printStackTrace(); }
		titleList = currentDoc.getElementsByTagName("title");
		pageID = LibraryUtils.getRand(0, titleList.getLength());
		chkNumber();
		display();
	}
	
	public static void display() {
		Element e = (Element) currentDoc.getElementsByTagName("title").item(pageID);
		String titletw = e.getElementsByTagName("titletw").item(0).getTextContent();
		String titlecn = e.getElementsByTagName("titlecn").item(0).getTextContent();
		String first = e.getElementsByTagName("first").item(0).getTextContent();

		tFile.setText(fileList[fileID].getPath());
		tTitleTW.setText(titletw);
		tTitleCN.setText(titlecn);
		tContent.setText(first);

		NodeList catList = e.getElementsByTagName("cat");

		int i;
		for (i = 0; i < catList.getLength(); i++) {
			CheckBox c;
			Node cat = catList.item(i);
			String ss = titletw + " 是一個 " + cat.getTextContent() + " 嗎?";
			if (i >= categoryList.size()) {
				c = new CheckBox(ss);
				categoryList.add(c);
				pCategory.getChildren().add(c);
			} else {
				c = categoryList.get(i);
				c.setText(ss);
				c.setVisible(true);
			}
			Node isa = cat.getAttributes().getNamedItem("isa");
			if (isa != null)
				c.setSelected(isa.getTextContent().equals("0") ? false : true);
			else
				c.setSelected(false);
		}
		for (; i < categoryList.size(); i++)
			categoryList.get(i).setVisible(false);
		isErrContent.setSelected(false);
		
		tRecordTotal.setText("Total Record: " + docRecord.getElementsByTagName("title").getLength());
	}

	public static void initFout() {
		File errFile = new File(dirOut + errFilePath);
		File recordFile = new File(dirOut + recordFilePath);
		
		try {
			if (!errFile.exists()) {
				docErr = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element errpage = docErr.createElement("errpage");
				docErr.appendChild(errpage);
			} else docErr = LibraryIO.loadXML(errFile);
		} catch (Exception e) { e.printStackTrace(); }
		
		try {
			if (!recordFile.exists()) { 
				docRecord = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element titleset = docRecord.createElement("titleset");
				docRecord.appendChild(titleset);
			} else docRecord = LibraryIO.loadXML(recordFile);
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public static void initDisplay() throws Exception {
		fileList = new File(dirInn).listFiles();
		fileID = LibraryUtils.getRand(0, fileList.length);

		currentDoc = LibraryIO.loadXML(fileList[fileID]);
		titleList = currentDoc.getElementsByTagName("title");

		pageID = LibraryUtils.getRand(0, titleList.getLength());

		categoryList = new ArrayList<>();
	}

	public static void initGUI(Stage primaryStage) {
		primaryStage.setTitle("A is a B?");

		isErrContent.setAlignment(Pos.CENTER_LEFT);
		isErrContent.setPadding(new Insets(10));

		// File Pane
		BorderPane pFile = new BorderPane();
		pFile.setLeft(t1);
		pFile.setCenter(tFile);
		BorderPane.setAlignment(t1, Pos.CENTER);
		pFile.setPadding(new Insets(10, 10, 0, 10));

		// Title TW Pane
		BorderPane pTitleTw = new BorderPane();
		pTitleTw.setLeft(t2);
		pTitleTw.setCenter(tTitleTW);
		pTitleTw.setRight(bCopy);
		BorderPane.setAlignment(t2, Pos.CENTER);
		pTitleTw.setPadding(new Insets(10));
		BorderPane.setMargin(bCopy, new Insets(10));

		// Title CN Pane
		BorderPane pTitleCn = new BorderPane();
		pTitleCn.setLeft(t3);
		pTitleCn.setCenter(tTitleCN);
		BorderPane.setAlignment(t3, Pos.CENTER);
		pTitleCn.setPadding(new Insets(10, 10, 0, 10));

		// Title Pane
		BorderPane pTitle = new BorderPane();
		pTitle.setLeft(pTitleTw);
		pTitle.setRight(pTitleCn);

		// Scroll Pane of Content
		ScrollPane sp = new ScrollPane();
		sp.setContent(tContent);
		sp.setMaxHeight(10);
		sp.setFitToWidth(true);
		sp.setFitToHeight(true);
		sp.setVbarPolicy(ScrollBarPolicy.NEVER);
		sp.setHbarPolicy(ScrollBarPolicy.ALWAYS);

		// Content Pane
		BorderPane pContent = new BorderPane();
		pContent.setTop(isErrContent);
		pContent.setCenter(sp);
		BorderPane.setAlignment(isErrContent, Pos.CENTER);
		pContent.setPadding(new Insets(0, 10, 0, 10));

		// Top Pane
		VBox vbTop = new VBox();
		vbTop.getChildren().add(pFile);
		vbTop.getChildren().add(pTitle);
		vbTop.getChildren().add(pContent);

		// Bottom Pane
		GridPane pBottom = new GridPane();
		pBottom.setHgap(10);
		pBottom.add(bNext, 0, 0);
		pBottom.add(bPre, 1, 0);
		pBottom.add(bRnd, 2, 0);
		pBottom.add(tRecordTotal, 3, 0);
		pBottom.setPadding(new Insets(10));

		// Category Pane
		pCategory.setPadding(new Insets(10));
		pCategory.setVgap(10);
		pCategory.setHgap(10);

		bNext.setPrefWidth((800 - 50) / 2);
		bNext.setPrefHeight(50);
		bPre.setPrefWidth((800 - 50) / 2);
		bPre.setPrefHeight(50);
		bRnd.setPrefWidth((800 - 50) / 2);
		bRnd.setPrefHeight(50);

		for (int i = 0; i < 4; i++) {
			ColumnConstraints cc = new ColumnConstraints();
			cc.setPercentWidth(100.0 / 4.0);
			cc.setHgrow(Priority.ALWAYS);
			pBottom.getColumnConstraints().add(cc);
		}

		// Outside Pane
		BorderPane p = new BorderPane();
		p.setTop(vbTop);
		p.setCenter(pCategory);
		p.setBottom(pBottom);
		BorderPane.setMargin(pFile, new Insets(20));

		Scene scene = new Scene(p, 800, 400);

		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void main(String[] args) {
		SegmentorApp.launch(args);
	}
}
