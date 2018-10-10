package es.bsc.inb.limtox.model;

public class CypTerm {

	private String uniProtEntry="";
	
	private String uniProtEntryName="";
	
	private String cypTermStandardized="";
	
	private String cypTerm="";

	
	
	public CypTerm(String uniProtEntry, String uniProtEntryName, String cypTermStandardized, String cypTerm) {
		super();
		this.uniProtEntry = uniProtEntry;
		this.uniProtEntryName = uniProtEntryName;
		this.cypTermStandardized = cypTermStandardized;
		this.cypTerm = cypTerm;
	}

	public String getUniProtEntry() {
		return uniProtEntry;
	}

	public void setUniProtEntry(String uniProtEntry) {
		this.uniProtEntry = uniProtEntry;
	}

	public String getUniProtEntryName() {
		return uniProtEntryName;
	}

	public void setUniProtEntryName(String uniProtEntryName) {
		this.uniProtEntryName = uniProtEntryName;
	}

	public String getCypTermStandardized() {
		return cypTermStandardized;
	}

	public void setCypTermStandardized(String cypTermStandardized) {
		this.cypTermStandardized = cypTermStandardized;
	}

	public String getCypTerm() {
		return cypTerm;
	}

	public void setCypTerm(String cypTerm) {
		this.cypTerm = cypTerm;
	}
	
	
	
}
