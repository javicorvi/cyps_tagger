package es.bsc.inb.limtox.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.MentionsAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.objectbank.ObjectBank;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import es.bsc.inb.limtox.model.CypTerm;


@Service
public class TaggingServiceImpl implements TaggingService{
	
	static final Logger taggingLog = Logger.getLogger("taggingLog");
	
	Map<String, CypTerm> cypTerms = new HashMap<String, CypTerm>();
	
	public void execute(String propertiesParametersPath) {
		try {
			taggingLog.info("Tagging cyps with properties :  " +  propertiesParametersPath);
			Properties propertiesParameters = this.loadPropertiesParameters(propertiesParametersPath);
			taggingLog.info("Input directory with the articles to tag : " + propertiesParameters.getProperty("inputDirectory"));
			taggingLog.info("Output directory : " + propertiesParameters.getProperty("outputDirectory"));
			taggingLog.info("cyps dictionary used : " + propertiesParameters.getProperty("cypsDict"));
			
			String inputDirectoryPath = propertiesParameters.getProperty("inputDirectory");
			String outputDirectoryPath = propertiesParameters.getProperty("outputDirectory");
			String cypsDict = propertiesParameters.getProperty("cypsDict");
			Integer index_id = new Integer(propertiesParameters.getProperty("index_id"));
			Integer index_text_to_tag = new Integer(propertiesParameters.getProperty("index_text_to_tag"));
			String taxonomyInformationPath = propertiesParameters.getProperty("taxonomyInformationPath");
			File inputDirectory = new File(inputDirectoryPath);
		    if(!inputDirectory.exists()) {
		    	return ;
		    }
		    if (!Files.isDirectory(Paths.get(inputDirectoryPath))) {
		    	return ;
		    }
		    File outputDirectory = new File(outputDirectoryPath);
		    if(!outputDirectory.exists())
		    	outputDirectory.mkdirs();
		    
		    String rulesPathOutput = "cyps_rules.txt";
		    generateRulesForTagging(cypsDict, rulesPathOutput);
		    
			Properties props = new Properties();
			props.put("annotators", "tokenize, ssplit, regexner, entitymentions");
			props.put("regexner.mapping", rulesPathOutput);
			props.put("regexner.posmatchtype", "MATCH_ALL_TOKENS");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	
			List<String> filesProcessed = readFilesProcessed(outputDirectoryPath); 
		    BufferedWriter filesPrecessedWriter = new BufferedWriter(new FileWriter(outputDirectoryPath + File.separator + "list_files_processed.dat", true));
		    File[] files =  inputDirectory.listFiles();
			for (File file_to_classify : files) {
				if(file_to_classify.getName().endsWith(".txt") && filesProcessed!=null && !filesProcessed.contains(file_to_classify.getName())){
					taggingLog.info("Processing file  : " + file_to_classify.getName());
					String fileName = file_to_classify.getName();
					String outputFilePath = outputDirectory + File.separator + fileName;
					
					
					String taxonomyInputInformation = taxonomyInformationPath + file_to_classify.getName()+"_tagged.txt";
					
					BufferedWriter outPutFile = new BufferedWriter(new FileWriter(outputFilePath));
					outPutFile.write("id\tstartOffset\tendOffset\ttext\tentityType\tuni_prot_entry_name\torganism\tcyp_standard\n");
					outPutFile.flush();
					for (String line : ObjectBank.getLineIterator(file_to_classify.getAbsolutePath(), "utf-8")) {
						try {
							String[] data = line.split("\t");
							String id =  data[index_id];
							String text =  data[index_text_to_tag];
							tagging(cypsDict, taxonomyInputInformation, pipeline, id, text, outPutFile, file_to_classify.getName());
						}  catch (Exception e) {
							taggingLog.error("Error tagging the document line " + line + " belongs to the file: " +  fileName,e);
						} 
					
					}
					outPutFile.close();
					filesPrecessedWriter.write(file_to_classify.getName()+"\n");
					filesPrecessedWriter.flush();
				}
			}
			filesPrecessedWriter.close();
		}  catch (Exception e) {
			taggingLog.error("Generic error in the classification step",e);
		} 
	}

	private void generateRulesForTagging(String inputPath,String outputPath) throws IOException {
		BufferedWriter termWriter = new BufferedWriter(new FileWriter(outputPath));
		Set<String> terms = new HashSet<String>();
		for (String line : ObjectBank.getLineIterator(inputPath, "utf-8")) {
			if(!line.startsWith("uniprot_entry_name")) {
				String[] data = line.split("\t");
				if(data[2].endsWith("_variant")) {
					//terms.add(data[4]);
					terms.add(this.removeInvalidCharacters(data[4]) + "\t" + data[2]+"_cyp\n");
				}else {
					//terms.add(data[3]);
					terms.add(this.removeInvalidCharacters(data[3]) + "\t" + data[2]+"_cyp\n");
				}
			}
		}
		for (String string : terms) {
			termWriter.write(string);
			termWriter.flush();
		}
		termWriter.close();
	}
	
	/**
	 * 
	 * @param original_entry
	 * @return
	 */
	private String removeInvalidCharacters(String original_entry) {
		return original_entry.replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\*", "");
	}



	
	
    
	
	/**
	 * Findings of LTKB ChemicalCompunds
	 * 
	 * @param sourceId
	 * @param document_model
	 * @param first_finding_on_document
	 * @param section
	 * @param sentence_text
	 * @return
	 * @throws MoreThanOneEntityException
	 */
	private void tagging(String dictInput,String taxonomyInputInformation, StanfordCoreNLP pipeline, String id, String text_to_tag, BufferedWriter output, String fileName) {
//		String text = "Joe Smith CYP2C8 was born in California. " +
//			      "In 2017, he went amineptine to Paris, France in the summer Cytochrome P450 4A12. " +
//			      "xenobiotic liver toxicity His flight left at 3:00pm on alatrofloxacin mesylate July 10th, 2017. " +
//			      "After eating some escargot for the first time, Joe said, \"That was delicious!\" " +
//			      "He sent xenobiotic liver toxicity a postcard to his sister Jane Smith, Cytochrome P450. " +
//			      "After Cytochrome P450 2D4 hearing about Joe's fipexide trip, Cyp3a-2 Jane decided she might go to France one day.";
//		//Annotation document = new Annotation(text);
		Annotation document = new Annotation(text_to_tag);
		// run all Annotators on this text
		pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
        	// traversing the words in the current sentence
	        // a CoreLabel is a CoreMap with additional token-specific methods
        	List<CoreMap> entityMentions = sentence.get(MentionsAnnotation.class);
    		for (CoreMap entityMention : entityMentions) {
    			try {
    				String keyword = entityMention.get(TextAnnotation.class);
        			String entityType = entityMention.get(CoreAnnotations.EntityTypeAnnotation.class);
			        if(entityType!=null && entityType.endsWith("_cyp")) {
			        	CoreLabel token = entityMention.get(TokensAnnotation.class).get(0);
			        	CypTerm cypTerm = this.findCypTermInformation(dictInput, taxonomyInputInformation, keyword, id);
			        	if(cypTerm!=null) {
			        		output.write(id + "\t"+ token.beginPosition() + "\t" + (token.beginPosition() + keyword.length())  + "\t" + keyword + "\t" + entityType + "\t" + 
			        				cypTerm.getUniProtEntryName() + "\t" + cypTerm.getOrganism() + "\t" + cypTerm.getCypFamily() + "\n");
			        	} else {
			        		//when the combiner is used a statically ner, that way is not present in the dictionary
			        		output.write(id + "\t"+ token.beginPosition() + "\t" + (token.beginPosition() + keyword.length())  + "\t" + keyword + "\t" + entityType + "\t null \t null \t null \n");
			        		taggingLog.warn("Entry not found " + keyword);
			        	}
			        	output.flush();
			        }else {
			        	entityType.toString();
			        }
        		} catch (Exception e) {
					taggingLog.error("Generic Error tagging id "  + id + " in file " + fileName, e);
				}
    		}
        }
	}

	/**
	 * 
	 * @param keyword
	 * @return
	 */
	private CypTerm findCypTermInformation(String inputPath, String taxonomyInputInformation, String keyword, String id) {
		List<CypTerm> cyps = new ArrayList<CypTerm>();
		for (String line : ObjectBank.getLineIterator(inputPath, "utf-8")) {
			if(!line.startsWith("uniprot_entry_name")) {
				String[] data = line.split("\t");
				if(data[2].endsWith("_variant")) {
					if(data[4].equals(keyword)) {
						cyps.add(new CypTerm(data[0],data[1],data[2],data[4],data[3]));
					}
				}else {
					if(data[3].equals(keyword)) {
						cyps.add(new CypTerm(data[0],data[1],data[2],data[3],""));
					}
				}
			}
		}
		if(cyps.size()>1) {
			for (String line : ObjectBank.getLineIterator(taxonomyInputInformation, "utf-8")) {
				String[] data = line.split("\t");
				if(data[1].equals(id)) {
					String[] organisms = data[0].split("\\|");
					for (String organism : organisms) {
						int i = organism.lastIndexOf(':');
						int i_ = organism.lastIndexOf('?');
						if(i_!=-1) {
							organism = organism.substring(i+1,i_-1);
						}else {
							organism = organism.substring(i+1);
						}
						for (CypTerm cypTerm : cyps) {
							String org_cys = cypTerm.getOrganism();
							int i2 = org_cys.lastIndexOf(':');
							org_cys = org_cys.substring(i2+1);
							if (organism.equals(org_cys)) {
								return cypTerm;
							}
						}
					}
				}
			}
			for (CypTerm cypTerm : cyps) {
				if(cypTerm.getUniProtEntryName().contains("_HUMAN")) {
					return cypTerm;
				}
			}
		}else if(cyps.size()==1){
			return cyps.get(0);
		
		
		}else if (cyps.size()==0) {//this means that the key founded was not generates in the variants and is from the nlp core tagger algorithm
			return null;
		}
		return null;
	}

	private List<String> readFilesProcessed(String outputDirectoryPath) {
		try {
			List<String> files_processed = new ArrayList<String>();
			if(Files.isRegularFile(Paths.get(outputDirectoryPath + File.separator + "list_files_processed.dat"))) {
				FileReader fr = new FileReader(outputDirectoryPath + File.separator + "list_files_processed.dat");
			    BufferedReader br = new BufferedReader(fr);
			    
			    String sCurrentLine;
			    while ((sCurrentLine = br.readLine()) != null) {
			    	files_processed.add(sCurrentLine);
				}
			    br.close();
			    fr.close();
			}
			return files_processed;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	  * Load Properties
	  * @param properitesParametersPath
	  */
	 public Properties loadPropertiesParameters(String properitesParametersPath) {
		 Properties prop = new Properties();
		 InputStream input = null;
		 try {
			 input = new FileInputStream(properitesParametersPath);
			 // load a properties file
			 prop.load(input);
			 return prop;
		 } catch (IOException ex) {
			 ex.printStackTrace();
		 } finally {
			 if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			 }
		}
		return null;
	 }	

	
}