import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


/*
 * Reads an input file with the following format:
 * <PercInsert> <PercModify> <PercSearch>
 * 
 * and an unknown number of lines as follows:
 * <T> <perc> <attr1> ... <attrN>
 * 
 * where T is the type of the operation: I(nsert), M(odification) and S(earch)
 */
public class QueryGenerator {

    private final TreeSet<QueryTemplate> templatesInsert;
    private final TreeSet<QueryTemplate> templatesModification;
    private final TreeSet<QueryTemplate> templatesSearch;

    private final double percInsert;
    private final double percModify;
    private final double percSearch;
    
    public QueryGenerator(String configFile) {
	List<String> configLines = readFileContent(configFile);
	if (configLines.size() <= 1) {
	    exitError("Must have at least a line for percentages of operations and one query example");
	}

	String[] percentagesConfig = configLines.get(0).split(" ");
	if (percentagesConfig.length != 3) {
	    exitError("Must configure 3 percentages for: <PercInsert> <PercModify> <PercSearch>");
	}

	this.percInsert = Double.parseDouble(percentagesConfig[0]);
	this.percModify = Double.parseDouble(percentagesConfig[1]);
	this.percSearch = Double.parseDouble(percentagesConfig[2]);
	if ((this.percInsert + this.percModify + this.percSearch) != 100.0) {
	    exitError("The sum of percentages of each type of operation must be 100.0");
	}

	this.templatesInsert = new TreeSet<QueryTemplate>();
	this.templatesModification = new TreeSet<QueryTemplate>();
	this.templatesSearch = new TreeSet<QueryTemplate>();

	Map<String, Double> percCreator = new HashMap<String, Double>();
	percCreator.put("I", 0.0);
	percCreator.put("M", 0.0);
	percCreator.put("S", 0.0);
	
	configLines.remove(0);
	for (String line : configLines) {
	    String[] template = line.split(" ");
	    if (template.length == 0) {
		continue;
	    }
	    if (template.length < 3) {
		exitError("Incorrect query template " + line);
	    }
	    
	    String type = template[0];
	    // use the last roof of this type as the basis of the interval for this template
	    double percentageTemplate = Double.parseDouble(template[1]) + percCreator.get(type);
	    List<String> attrs = new ArrayList<String>();
	    for (int i = 2; i < template.length; i++) {
		String attributeName = template[i];
		attrs.add(attributeName);
	    }
	    
	    TreeSet<QueryTemplate> typeTemplates = getTypeTemplates(type);
	    typeTemplates.add(new QueryTemplate(percentageTemplate, attrs));
	    // update the roof for this type
	    percCreator.put(type, percentageTemplate);
	}

	double tmpPercInsert = percCreator.get("I");
	double tmpPercModify = percCreator.get("M");
	double tmpPercSearch = percCreator.get("S");
	
	if (tmpPercInsert != 100.0 || tmpPercInsert == 0.0 && this.percInsert != 0.0) {
	    exitError("The sum of percentages for templates of inserts is not 100.0");
	}
	if (tmpPercModify != 100.0 || tmpPercModify == 0.0 && this.percModify != 0.0) {
	    exitError("The sum of percentages for templates of modify is not 100.0");
	}
	if (tmpPercSearch != 100.0 || tmpPercSearch == 0.0 && this.percSearch != 0.0) {
	    exitError("The sum of percentages for templates of search is not 100.0");
	}
    }

    private TreeSet<QueryTemplate> getTypeTemplates(String type) {
	if (type.equals("I")) {
	    return this.templatesInsert;
	} else if (type.equals("M")) {
	    return this.templatesModification;
	} else {
	    return this.templatesSearch;
	}
    }
    
    private List<String> readFileContent(String filename) {
	List<String> lines = new ArrayList<String>();
	try {
	    FileInputStream is = new FileInputStream(filename);
	    DataInputStream in = new DataInputStream(is);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
	    String strLine;
	    while ((strLine = br.readLine()) != null) {
		if (strLine.equals("")) {
		    continue;
		}
		lines.add(strLine);
	    }
	    br.close();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	return lines;
    }

    private void exitError(String error) {
	System.err.println(error);
	System.exit(1);
    }

    /*
     * A QueryTemplate <Q> is parametrized by a percentage roof <R>.
     * There is a set of Qs: Q1, Q2, Q3
     * 
     * The interval for which Q1 is responsible is ]0, Q1.R]
     * For Q2 it is: ]Q1.R, Q2.R]
     * And for Q3: ]Q2.R, Q3.R]
     * 
     * Invariant: the last Q, namely Q3 in this example, should have R = 100.0
     */
    private class QueryTemplate implements Comparable<QueryTemplate> {

	private final double percentageRoof;
	private final List<String> attributesToQuery;

	QueryTemplate(double percentageRoof, List<String> attributes) {
	    this.percentageRoof = percentageRoof;
	    this.attributesToQuery = attributes;
	}

	@Override
	public int compareTo(QueryTemplate other) {
	    if (this.percentageRoof > other.percentageRoof) {
		return 1;
	    } else if (this.percentageRoof == other.percentageRoof) {
		return 0;
	    } else {
		return -1;
	    }
	}

    }
}
