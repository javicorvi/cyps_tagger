package es.bsc.inb.limtox.model;

public class CypTerm {

	private String uniProtEntryName="";
	
	private String organism="";
	
	private String type="";
	
	private String cypTerm="";

	private String cypFamily="";

	public CypTerm(String uniProtEntryName, String organism, String type, String cypTerm, String cypFamily) {
		super();
		this.uniProtEntryName = uniProtEntryName;
		this.organism = organism;
		this.type = type;
		this.cypTerm = cypTerm;
		this.cypFamily = cypFamily;
	}

	public String getUniProtEntryName() {
		return uniProtEntryName;
	}

	public void setUniProtEntryName(String uniProtEntryName) {
		this.uniProtEntryName = uniProtEntryName;
	}

	public String getOrganism() {
		return organism;
	}

	public void setOrganism(String organism) {
		this.organism = organism;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCypTerm() {
		return cypTerm;
	}

	public void setCypTerm(String cypTerm) {
		this.cypTerm = cypTerm;
	}

	public String getCypFamily() {
		return cypFamily;
	}

	public void setCypFamily(String cypFamily) {
		this.cypFamily = cypFamily;
	}
	
	

	
	
	
}
