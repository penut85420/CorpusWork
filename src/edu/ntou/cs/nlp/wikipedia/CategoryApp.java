package edu.ntou.cs.nlp.wikipedia;

import java.io.File;

import org.oppai.io.LibraryIO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.ntou.cs.nlp.wordSegmentation.ui.SegmentorApp;
import javafx.application.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

public class CategoryApp extends Application {
	
	static Label t1 = new Label("File:  ");
	static Label t2 = new Label("Title TW:  ");
	static Label t3 = new Label("Title CN:  ");
	static Label t4 = new Label("First Paragraph");
	
	static TextField tFile = new TextField();
	static TextField tTitleTW = new TextField();
	static TextField tTitleCN = new TextField();
	static TextArea tContent = new TextArea();
	static TextField tIsA = new TextField();
	
	static Button bYes = new Button("Yes");
	static Button bNo  = new Button("No");
	static Button bSave = new Button("Save");
	static Button bClose = new Button("Close");
	
	static File[] flist;
	static Document doc;
	static NodeList nd;
	static int fid;
	static int nd_i;
	static int cat_i;
	
	final static String dirInn = "D:\\Documents\\Corpus\\Wiki\\TitleList\\";
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		initGUI(primaryStage);
		initDisplay();
		display();
		
		bYes.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				((Element) ((Element) doc.getElementsByTagName("title").item(nd_i)).getElementsByTagName("cat")
						.item(cat_i)).setAttribute("isa", "Y");
				cat_i++;
				chkNumber();
				display();
			}
		});
	}
	
	public static void chkNumber() {
		Element e = (Element)doc.getElementsByTagName("title").item(nd_i);
		NodeList nnd = e.getElementsByTagName("cat");
		while (cat_i >= nnd.getLength()) {
			nd_i++;
			cat_i = 0;
			if (nd_i >= nd.getLength()) {
				fid++;
				try { doc = LibraryIO.loadXML(flist[fid]); }
				catch (Exception ee) { ee.printStackTrace(); }
				nd_i = 0;
			}
			System.out.println(nd_i + " ");
			nnd = ((Element)doc
					.getElementsByTagName("title")
					.item(nd_i))
					.getElementsByTagName("cat");
		}
	}
	
	public static void display() {
		Element e = (Element)doc.getElementsByTagName("title").item(nd_i);
		String titletw = e.getElementsByTagName("titletw").item(0).getTextContent();
		String titlecn = e.getElementsByTagName("titlecn").item(0).getTextContent();
		String first = e.getElementsByTagName("first").item(0).getTextContent();
		String cat = e.getElementsByTagName("cat")
				.item(cat_i).getTextContent();
		
		tFile.setText(dirInn + "zhwikiMain_0001.xml");
		tTitleTW.setText(titletw);
		tTitleCN.setText(titlecn);
		tContent.setText(first);
		tIsA.setText(titletw + " is a " + cat + "?");
	}
	
	public static void initDisplay() throws Exception {
		flist = new File(dirInn).listFiles();
		fid = 0;
		
		doc = LibraryIO.loadXML(flist[fid]);
		nd = doc.getElementsByTagName("title");
		
		nd_i = 0;
		cat_i = 0;
	}
	
	public static void initGUI(Stage primaryStage) {
		primaryStage.setTitle("A is a B?");
		
		t4.setAlignment(Pos.CENTER_LEFT);
		
		// File Pane
		BorderPane pFile = new BorderPane();
		pFile.setLeft(t1);
		pFile.setCenter(tFile);
		BorderPane.setAlignment(t1, Pos.CENTER);
		pFile.setPadding(new Insets(20));
		
		// Title TW Pane
		BorderPane pTitleTw = new BorderPane();
		pTitleTw.setLeft(t2);
		pTitleTw.setCenter(tTitleTW);
		BorderPane.setAlignment(t2, Pos.CENTER);
		pTitleTw.setPadding(new Insets(20));
		
		// Title CN Pane
		BorderPane pTitleCn = new BorderPane();
		pTitleCn.setLeft(t3);
		pTitleCn.setCenter(tTitleCN);
		BorderPane.setAlignment(t3, Pos.CENTER);
		pTitleCn.setPadding(new Insets(20));
		
		// Title Pane
		BorderPane pTitle = new BorderPane();
		pTitle.setLeft(pTitleTw);
		pTitle.setRight(pTitleCn);
		
		// Top Pane
		VBox vbTop = new VBox();
		vbTop.getChildren().add(pFile);
		vbTop.getChildren().add(pTitle);
		
		// Content Pane
		BorderPane pContent = new BorderPane();
		pContent.setTop(t4);
		pContent.setCenter(tContent);
		pContent.setBottom(tIsA);
		BorderPane.setAlignment(t4, Pos.CENTER);
		pContent.setPadding(new Insets(10));
		
		// Button Pane
		GridPane pButton = new GridPane();
		pButton.add(bYes, 0, 0);
		pButton.add(bNo, 1, 0);
		pButton.add(bSave, 2, 0);
		pButton.add(bClose, 3, 0);
		
		bYes.setPrefWidth((800-50)/4);
		bYes.setPrefHeight(50);
		bNo.setPrefWidth((800-50)/4);
		bNo.setPrefHeight(50);
		bSave.setPrefWidth((800-50)/4);
		bSave.setPrefHeight(50);
		bClose.setPrefWidth((800-50)/4);
		bClose.setPrefHeight(50);
		
		for (int i = 0 ; i < 4 ; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0/4.0);
            cc.setHgrow(Priority.ALWAYS);
            pButton.getColumnConstraints().add(cc);
        }
		
		pButton.setPadding(new Insets(10));
		
		// Outside Pane
		BorderPane p = new BorderPane();
		p.setTop(vbTop);
		p.setCenter(pContent);
		p.setBottom(pButton);
		BorderPane.setMargin(pFile, new Insets(20));
		
		Scene scene = new Scene(p, 800, 600);
		
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		SegmentorApp.launch(args);
	}
}
