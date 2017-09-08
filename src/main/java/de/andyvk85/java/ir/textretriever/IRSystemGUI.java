package de.andyvk85.java.ir.textretriever;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.apache.lucene.util.Version;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

public class IRSystemGUI extends Application implements Initializable {

    private Stage primaryStage;
    private AnchorPane rootLayout;
    private List<ScoreDoc> data = new ArrayList<ScoreDoc>();
    private String dirToBeIndexed;
    private Directory indexDir;
    private File dataDir;
    private ObservableList<ScoreDoc> myObservableList;
    private String searchQuery;
    private int searchLimit;

    // elements from fxml design file injected by fxmlloader
    @FXML
    private Button setDocFolderBtn;
    @FXML
    private Button startIndexingBtn;
    @FXML
    private Button searchBtn;
    @FXML
    private Label docFolderLbl;
    @FXML
    private Label wasIndexedLbl;
    @FXML
    private Label rankLbl;
    @FXML
    private Label scoreLbl;
    @FXML
    private Label lastModifiedLbl;
    @FXML
    private TextField searchLimitTf;
    @FXML
    private TextField searchQueryTf;
    @FXML
    private ListView<ScoreDoc> resultsLV;

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("TextRetriever (by andyvk85)");
        this.primaryStage = primaryStage;

        try {
            // load root layout from fxml file
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("IRSystemGUI.fxml"));
            rootLayout = (AnchorPane) loader.load();

            // show the scene containing the root layout
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void handleIndexing(ActionEvent event) {
        indexDir = new RAMDirectory();
        dataDir = new File(dirToBeIndexed);

        try {
            int numIndexed = index(indexDir, dataDir);
            wasIndexedLbl.setText("total files indexed " + numIndexed + "..");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleSearch(ActionEvent event) {
        resultsLV.getSelectionModel().clearSelection();
        resultsLV.getItems().clear();
        data.clear();
        myObservableList.clear();
        resultsLV.setItems(myObservableList);

        searchQuery = searchQueryTf.getText();

        try {
            search(indexDir, searchQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }

        myObservableList = FXCollections.observableList(data);
        resultsLV.setItems(myObservableList);
    }


    public void handleDocFolder(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory == null) {
            docFolderLbl.setText("No Document Directory selected");
        } else {
            docFolderLbl.setText(selectedDirectory.getAbsolutePath());
            dirToBeIndexed = selectedDirectory.getAbsolutePath();
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void initialize(URL location, ResourceBundle resources) {

        myObservableList = FXCollections.observableList(data);
        resultsLV.setItems(myObservableList);

        resultsLV.setCellFactory(new Callback<ListView<ScoreDoc>, ListCell<ScoreDoc>>() {
            public ListCell<ScoreDoc> call(ListView<ScoreDoc> p) {
                ListCell<ScoreDoc> cell = new ListCell<ScoreDoc>() {

                    @Override
                    protected void updateItem(ScoreDoc t, boolean bln) {
                        super.updateItem(t, bln);
                        if (t != null) {

                            IndexReader indexReader;
                            IndexSearcher searcher;
                            try {
                                indexReader = DirectoryReader.open(indexDir);
                                searcher = new IndexSearcher(indexReader);
                                int docId = t.doc;
                                Document d = new Document();
                                d = searcher.doc(docId);
                                setText(d.getField("fileName").stringValue());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }

                };

                return cell;
            }
        });

        resultsLV.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<ScoreDoc>() {
            public void changed(ObservableValue<? extends ScoreDoc> observable, ScoreDoc oldValue, ScoreDoc newValue) {

                if(newValue!=null) {
                    IndexReader indexReader;
                    IndexSearcher searcher;
                    try {
                        indexReader = DirectoryReader.open(indexDir);
                        searcher = new IndexSearcher(indexReader);
                        int docId = newValue.doc;
                        Document d = new Document();
                        d = searcher.doc(docId);

                        long lastModDateMilli = Long.parseLong(d.get("lastModTime"));
                        DateFormat dateFormatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.GERMANY);
                        String lastModDate = dateFormatter.format(Date.from(Instant.ofEpochMilli(lastModDateMilli)));
                        DecimalFormat scoreFormatter = new DecimalFormat("##.####");
                        String score = scoreFormatter.format(newValue.score);
                        int rank = data.indexOf(newValue) + 1;

                        rankLbl.setText(Integer.toString(rank));
                        scoreLbl.setText(score);
                        lastModifiedLbl.setText(lastModDate);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    private void search(Directory indexDir, String queryStr) throws Exception {

		/*
		 * search process:
		 *   - process query string with standard analyzer
		 *   - access index with IndexReader, IndexSearcher
		 *   - searcher needs query instance (we use a phase query)
		 *   - get 20 relevant documents with its score
		 */
        IndexReader indexReader = DirectoryReader.open(indexDir);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);

        QueryBuilder builder = new QueryBuilder(analyzer);
        Query query = builder.createPhraseQuery("content", queryStr);
        searchLimit = Integer.parseInt(searchLimitTf.getText());
        TopDocs topDocs = searcher.search(query, searchLimit);
        ScoreDoc[] hits = topDocs.scoreDocs;

        for (int i = 0; i < hits.length; i++) {
            data.add(hits[i]);
        }
    }

    private int index(Directory indexDir, File dataDir) throws IOException {

		/*
		 * index process:
		 *   - use standard analyzer (tokenizer and stemmer are included)
		 *   - interacting with the index requires an indexWriter
		 *   - parsing right files (.txt, .html) for the entire directory
		 *     structure is provided by listFileTree(..)
		 *   - each document in the index has the fields:
		 *     + content (not stored, only indexed)
		 *     + fileName (stored)
		 *     + lastModTime (stored)
		 */
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46, analyzer);
        IndexWriter indexWriter = new IndexWriter(indexDir, config);
        TextHTMLFileFilter filter = new TextHTMLFileFilter();

        ArrayList<File> files = listFileTree(dataDir, filter);

        for (File f : files) {
            Document doc = new Document();

            doc.add(new org.apache.lucene.document.TextField("content", new FileReader(f)));
            doc.add(new StoredField("fileName", f.getCanonicalPath()));
            doc.add(new StoredField("lastModTime", f.lastModified()));

            indexWriter.addDocument(doc);
        }

        int numIndexed = indexWriter.maxDoc();

        indexWriter.close();

        return numIndexed;
    }

    private static ArrayList<File> listFileTree(File dir, FileFilter filter) {

		/*
		 * file handling:
		 *   - parsing correct files with a TextHTMLFileFilter
		 *   - traverse recursively the directory structure
		 */
        ArrayList<File> fileTree = new ArrayList<File>();
        File[] files = dir.listFiles();

        if (dir == null || files == null) {
            return fileTree;
        }

        for (File f : files) {
            if (f.isFile() && filter.accept(f) && !f.isHidden() && f.exists() && f.canRead())
                fileTree.add(f);
            else //could be a directory
                fileTree.addAll(listFileTree(f, filter));
        }

        return fileTree;
    }
}
