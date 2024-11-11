package transformadores;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.json.JSONArray;
import org.json.JSONObject;
//import com.opencsv.CSVReader;

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
        	return "es un csv";
            //return transformCSVToJSON(fichero);
        } else if (fileType.equalsIgnoreCase("json")) {
            return fichero; // Si es JSON, simplemente devolvemos el contenido
        } else {
            throw new IllegalArgumentException("Formato de archivo no soportado");
        }
    }

    // Método que transforma contenido XML a JSON
    private String transformXMLToJSON(String xmlContent) throws Exception {
        Document document = DocumentHelper.parseText(xmlContent);  // Parsear XML
        JSONObject rootJson = new JSONObject();
        JSONArray actosArray = new JSONArray();
        rootJson.put("actos-evaluacion", actosArray);

        List<? extends Node> actos = document.selectNodes("//actoEvaluacion");
        for (Node acto : actos) {
            JSONObject actoJson = new JSONObject();
            actoJson.put("asignatura", acto.selectSingleNode("asignatura").getStringValue());
            actoJson.put("nombre", acto.selectSingleNode("nombre").getStringValue());

            JSONArray notasArray = new JSONArray();
            List<? extends Node> alumnos = acto.selectNodes("alumno");

            for (Node alumno : alumnos) {
                String nota = alumno.valueOf("@dni") + ":" + alumno.getStringValue();
                notasArray.put(nota);
            }

            actoJson.put("notas", notasArray);
            actosArray.put(actoJson);
        }

        return rootJson.toString(4); // Retorna el JSON con formato legible
    }

    // Método que transforma contenido CSV a JSON
/*    private String transformCSVToJSON(String csvContent) throws Exception {
        Map<String, JSONObject> actosMap = new HashMap<>();
        
        try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
            String[] nextLine;
            reader.readNext(); // Saltar la primera línea si es el encabezado

            while ((nextLine = reader.readNext()) != null) {
                String asignatura = nextLine[0];
                String acto = nextLine[1];
                String alumno = nextLine[2];
                String nota = nextLine[3];

                String key = asignatura + ":" + acto;
                JSONObject actoJson = actosMap.getOrDefault(key, new JSONObject());
                if (!actoJson.has("asignatura")) {
                    actoJson.put("asignatura", asignatura);
                    actoJson.put("nombre", acto);
                    actoJson.put("notas", new JSONArray());
                }

                JSONArray notasArray = actoJson.getJSONArray("notas");
                notasArray.put(alumno + ":" + nota);
                actosMap.put(key, actoJson);
            }
        }

        JSONObject rootJson = new JSONObject();
        JSONArray actosArray = new JSONArray();
        actosMap.values().forEach(actosArray::put);
        rootJson.put("actos-evaluacion", actosArray);

        return rootJson.toString(4); // Retorna el JSON con formato legible
    }*/

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
