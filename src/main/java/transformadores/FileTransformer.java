package transformadores;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.json.JSONArray;
import org.json.JSONObject;
import com.opencsv.CSVReader;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

public class FileTransformer extends AbstractMessageTransformer {

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException {
        String fichero = (String) message.getPayload();
        
        try {
            return transformToJSON(fichero); // Llamamos al método que convierte el contenido a JSON
        } catch (Exception e) {
            throw new TransformerException(this, e); // Lanza excepción si ocurre un error
        }
    }

    // Método que convierte el contenido a JSON dependiendo del tipo de archivo
    public String transformToJSON(String fichero) throws Exception {
        String fileType = determineFileType(fichero);
        if (fileType.equalsIgnoreCase("xml")) {
            return transformXMLToJSON(fichero);
        } else if (fileType.equalsIgnoreCase("csv")) {
            return transformCSVToJSON(fichero);
        } else if (fileType.equalsIgnoreCase("json")) {
            return transformJSONtoCFjson(fichero); // Modificar el JSON para ponerlo en Formato Canónico
        } else {
            throw new IllegalArgumentException("Formato de archivo no soportado");
        }
    }

    // Método que transforma el fichero XML a JSON
    private String transformXMLToJSON(String xmlFichero) throws Exception {
        Document document = DocumentHelper.parseText(xmlFichero);  // Parsea el XML contenido en la variable
        JSONArray actosArray = new JSONArray(); // Crea el array de JSONs principal.

        List<? extends Node> actos = document.selectNodes("//actoEvaluacion");
        for (Node acto : actos) {
            
            
            String asignatura = acto.selectSingleNode("asignatura").getStringValue();
            String nombre = acto.selectSingleNode("nombre").getStringValue(); 
     
        
            List<? extends Node> alumnos = acto.selectNodes("alumno");

            for (Node alumno : alumnos) {
            	JSONObject actoJson = new JSONObject();
            	actoJson.put("asignatura", asignatura);
            	actoJson.put("nombre", nombre);
                actoJson.put("dni", alumno.valueOf("@dni"));
                
                //Para que en el JSON no aparezca la nota como una cadena
                double nota = Double.parseDouble(alumno.getStringValue().trim());
                actoJson.put("nota", nota);
                
                actosArray.put(actoJson);
            }
        }
        JSONObject rootJson = new JSONObject();
        rootJson.put("evaluaciones", actosArray);
        return rootJson.toString(4); // Retorna el JSON con 4 espacios de identación
        // Tal vez si despues va a ser leido por la BD es interesante ponerlo con 0 espacios...
    }
    
    private String transformJSONtoCFjson(String JSONfichero) throws Exception {
    	JSONObject jsonEntrada = new JSONObject(JSONfichero); // Convertir el JSON de entrada en un objeto JSON
    	
    	JSONArray evaluacionesArray = new JSONArray(); // Crear el array de JSON evaluaciones
    	// Obtenemos el array "actos-evaluacion" del JSON de entrada
    	JSONArray actosArray = jsonEntrada.getJSONArray("actos-evaluacion"); 
    	
    	// Recorremos ahora cada acto-evaluacion
    	for (int i = 0; i < actosArray.length(); i++) {
    		JSONObject acto = actosArray.getJSONObject(i);
    		// Aqui extraemos asignatura y el nombre del acto evaluativo
    		String asignatura = acto.getString("asignatura");
    		String nombre = acto.getString("nombre");
    		// Obtenemos el subarray de notas
    		JSONArray notasArray = acto.getJSONArray("notas");
    		// Recorremos el subarray para obtener cada nota
    		for (int j = 0; j < notasArray.length(); j++) {
    			String entradaNota = notasArray.getString(j);
    			// Dividimos ahora en partes
    			String[] partes = entradaNota.split(":");
    			String dni = partes[0];
    			double nota = Double.parseDouble(partes[1]);
    			// Creamos el JSON en formato canónico
    			JSONObject cfJSON = new JSONObject();
    			cfJSON.put("asignatura", asignatura);
    			cfJSON.put("nombre", nombre);
    			cfJSON.put("dni", dni);
    			cfJSON.put("nota", nota);
    			
    			// Añadir la evaluacion al array de evaluaciones
    			evaluacionesArray.put(cfJSON);
    		}
    	}
    	// Creamos el JSON raíz (Recuerda que es un array de JSON)
    	JSONObject rootJSON = new JSONObject();
    	rootJSON.put("evaluaciones", evaluacionesArray);
    	return rootJSON.toString(4); // Devolver el JSON en formato identado
    }

    // Método que transforma contenido CSV a CFJSON
   private String transformCSVToJSON(String csvContent) throws Exception {
    JSONArray evaluacionesArray = new JSONArray();

    try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
        String[] nextLine;
        boolean isHeader = true; // Bandera para saltar el encabezado

        while ((nextLine = reader.readNext()) != null) {
            if (isHeader) {
                isHeader = false; // Saltar la primera línea
                continue;
            }

            // Validamos que la línea tenga al menos 4 columnas
            if (nextLine.length < 4) {
                throw new IllegalArgumentException("Formato CSV inválido, faltan columnas.");
            }

            // Extraemos los datos de cada columna
            String asignatura = nextLine[0].trim();
            String nombre = nextLine[1].trim();
            String dni = nextLine[2].trim();
            double nota = Double.parseDouble(nextLine[3].trim());

            // Creamos un objeto JSON para cada fila
            JSONObject evaluacionJson = new JSONObject();
            evaluacionJson.put("asignatura", asignatura);
            evaluacionJson.put("nombre", nombre);
            evaluacionJson.put("dni", dni);
            evaluacionJson.put("nota", nota);

            // Añadimos la evaluación al array principal
            evaluacionesArray.put(evaluacionJson);
        }
    }

    // Creamos el objeto raíz
    JSONObject rootJson = new JSONObject();
    rootJson.put("evaluaciones", evaluacionesArray);

    return rootJson.toString(4); // Retorna el JSON con 4 espacios de indentación
}


    // Método para determinar el tipo de archivo (XML, CSV, JSON)
    private String determineFileType(String content) {
        if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
            return "json";
        } else if (content.trim().startsWith("<")) {
            return "xml";
        } else if (content.contains(",") && content.contains("\n")) {
            return "csv";
        } else {
            throw new IllegalArgumentException("No se pudo determinar el tipo de archivo");
        }
    }
}
