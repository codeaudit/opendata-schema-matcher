package eu.trentorise.schemamatcher.implementation.services;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import au.com.bytecode.opencsv.CSVReader;
import eu.trentorise.opendata.columnrecognizers.ColumnRecognizer;
import eu.trentorise.opendata.disiclient.model.knowledge.ConceptODR;
import eu.trentorise.opendata.disiclient.services.WebServiceURLs;
import eu.trentorise.opendata.semantics.model.entity.IAttributeDef;
import eu.trentorise.opendata.semantics.model.entity.IEntityType;
import eu.trentorise.opendata.semantics.model.knowledge.IResourceContext;
import eu.trentorise.opendata.semantics.model.knowledge.ITableResource;
import eu.trentorise.schemamatcher.implementation.model.ElementContent;
import eu.trentorise.schemamatcher.implementation.model.ElementContext;
import eu.trentorise.schemamatcher.implementation.model.Schema;
import eu.trentorise.schemamatcher.implementation.model.SchemaElement;
import eu.trentorise.schemamatcher.implementation.model.SchemaElementFeatureExtractor;
import eu.trentorise.schemamatcher.implementation.model.SchemaMatcherException;
import eu.trentorise.schemamatcher.model.IResource;
import eu.trentorise.schemamatcher.model.ISchema;
import eu.trentorise.schemamatcher.model.ISchemaElement;
import eu.trentorise.schemamatcher.services.importing.ISchemaImport;

public class SchemaImport implements ISchemaImport{

	@Override
	public ISchema extractSchema(File file) {


		//TODO check file format 
		ISchema schemaOut = null;
		try {
			schemaOut = parseCSV(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return schemaOut;
	}

	/**
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public ISchema parseCSV(File file) throws IOException {

		CSVReader reader = new CSVReader(new FileReader(file));
		//String [] nextLine;
		List<String[]> strings = reader.readAll();
		String[] str = strings.get(0);
		System.out.println( str.length);
		List<ISchemaElement> sElements = new ArrayList<ISchemaElement>();
		for (int i=0; i<str.length;i++){
			SchemaElement scheamaElement = new SchemaElement();
			ElementContext sContext=new ElementContext();
			sContext.setElementName(str[i]);
			scheamaElement.setElementContext(sContext);

			ElementContent elContent = new ElementContent();
			List<Object> elInstances = new ArrayList<Object>();

			int lineNumber=0;
			for(String[] sArr: strings ){
				elInstances.add(sArr[i]);				
				lineNumber++;
			}
			elContent.setContentSize(lineNumber);
			elContent.setContent(elInstances);
			scheamaElement.setElementContent(elContent);
			sElements.add(scheamaElement);
		}
		Schema schema = new Schema();
		schema.setSchemaElements(sElements);
		//TODO change to extract concept from name
		Long conceptId = 2923L;
		schema.setSchemaConcept(conceptId);
		System.out.println(sElements.get(0).toString());
		return schema;
	}	
	/* (non-Javadoc)
	 * @see eu.trentorise.schemamatcher.services.importing.ISchemaImport#extractSchema(java.lang.Object)
	 */
	@Override
	public ISchema extractSchema(Object schema, Locale locale) throws SchemaMatcherException {

		if(schema==null){
			throw new SchemaMatcherException("Null input is provided!");
		}
		if(schema instanceof IResource){

			IResource resource = (IResource) schema;
			ISchema schemaOut= extractFromODRResource(resource.getResourceContext(), resource.getTableResource());
			return schemaOut;
		}
		if ((schema instanceof IEntityType)){
			IEntityType etype = (IEntityType) schema;

			Schema schemaOut = new Schema();
			String name = etype.getName().getString(locale);
			schemaOut.setSchemaName(name);
			schemaOut.setEtype(etype);
			List<ISchemaElement>	schemaElements=null;
			if(etype.getAttributeDefs().size()!=0){
				schemaElements=extractSchemaElements(etype, locale);
			}
			else {
				schemaElements=new ArrayList<ISchemaElement>();	
			}
			
			ConceptODR codr = new ConceptODR();
			codr = codr.readConceptGlobalID(etype.getConcept().getGUID());
			long globalConceptID =codr.getId();
			System.out.println("Etype: "+etype.getName().getString(Locale.ENGLISH) +" conce "+ globalConceptID);
			schemaOut.setSchemaConcept(globalConceptID);
			schemaOut.setSchemaElements(schemaElements);
			return schemaOut;
		}
		else
			throw new SchemaMatcherException("Unsupported schema format!");

	}

	private ISchema extractFromODRResource( IResourceContext resourceContext,ITableResource tableResource){
		Schema schema= new Schema();	

		schema.setSchemaName(resourceContext.getResourceName());
		List<ISchemaElement> selements = new ArrayList<ISchemaElement>();

		List<String> elNames= tableResource.getHeaders();
		for(String name: elNames){
			SchemaElement sElement = new SchemaElement();
			ElementContext elContext = new ElementContext();
			elContext.setElementName(name);
			sElement.setElementContext(elContext);
			selements.add(sElement);
		}
		SchemaElementFeatureExtractor sefe = new SchemaElementFeatureExtractor();
	// Assignment concepts for schema
		selements = sefe.runColumnRecognizer(selements);
		schema.setSchemaElements(selements);
		long schemaConceptID=ColumnRecognizer.conceptFromText(resourceContext.getResourceName());
		schema.setSchemaConcept(schemaConceptID);
		
		return schema;
	}



	private List<ISchemaElement> extractSchemaElements(IEntityType etype,Locale locale){
		List<IAttributeDef> attrDefs = etype.getAttributeDefs();

		List<ISchemaElement> schemaElements = new ArrayList<ISchemaElement>();

		for (IAttributeDef atrDef: attrDefs){

			SchemaElement schemaElement = new SchemaElement();

			ElementContext elContext = new ElementContext();
			ElementContent elContent = new ElementContent();
			//context extraction
			schemaElement.setAttrDef(atrDef);
			elContext.setElementName(atrDef.getName().getString(locale));
			elContext.setElemetnDataType(atrDef.getDataType());

			//convert local concept id to a global one
			//Long localConceptID = WebServiceURLs.urlToConceptID(atrDef.getConceptURL());

			//			ConceptODR codr = new ConceptODR();
			//			codr = codr.readConceptGlobalID(ccc.getConceptID());

			elContext.setElementConcept(WebServiceURLs.urlToConceptID(atrDef.getConceptURL()));
			schemaElement.setElementContext(elContext);
			//schema element relation extraction
			//TODO extract the schema structure
			//List<IElementRelation> elementRelation = new ArrayList<IElementRelation>();
			//schemaElement.setSchemaElementRelations(elementRelation);

			//schema element's content extraction
			//TODO find the way to download best representing content 

			schemaElements.add(schemaElement);
		}
		return schemaElements;
	}


}