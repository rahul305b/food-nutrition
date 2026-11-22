package com.rahulshankar.dialysisapp.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class DemoFoodImporter {
    private static final List<String> REQUIRED_HEADERS = List.of(
            "demo_id","food_name","local_name_tamil","category","preparation","basis",
            "energy_kcal","protein_g","carbohydrates_g","fat_g","fibre_g",
            "potassium_mg","phosphorus_mg","sodium_mg","calcium_mg","magnesium_mg",
            "iron_mg","data_source","source_record_id","data_status","citation","license_note");

    public ImportResult importCsv(InputStream input) throws Exception {
        if (input == null) throw new FileNotFoundException("Dataset CSV not found.");
        ImportResult result = new ImportResult();
        List<List<String>> records = readCsv(input);
        if (records.isEmpty()) throw new IOException("CSV file is empty.");
        List<String> headers = records.get(0);
        Map<String,Integer> indexes = new HashMap<>();
        for (int i=0;i<headers.size();i++) indexes.put(headers.get(i).trim().replace("\uFEFF",""),i);
        for (String required : REQUIRED_HEADERS) {
            if (!indexes.containsKey(required)) throw new IOException("Missing required CSV column: " + required);
        }

        try (Connection c=DemoFoodDatabase.connect()) {
            c.setAutoCommit(false);
            try {
                for (int rowNumber=1; rowNumber<records.size(); rowNumber++) {
                    List<String> row=records.get(rowNumber);
                    if (row.size()==1 && row.get(0).isBlank()) continue;
                    if (row.size()!=headers.size()) throw new IOException("CSV row " +(rowNumber+1)+
                            " has "+row.size()+" fields; expected "+headers.size());
                    String id=get(row,indexes,"demo_id"), name=get(row,indexes,"food_name");
                    if(id.isBlank()||name.isBlank()) throw new IOException("CSV row "+(rowNumber+1)+": demo_id and food_name are required.");
                    try(PreparedStatement check=c.prepareStatement("SELECT 1 FROM demo_imported_foods WHERE demo_id=?")){
                        check.setString(1,id);
                        try(ResultSet rs=check.executeQuery()){if(rs.next()){result.skipped++;continue;}}
                    }
                    insertRow(c,row,indexes,id,name,rowNumber+1);
                    result.imported++;
                }
                c.commit();
            } catch(Exception ex) { c.rollback(); throw ex; }
            finally { c.setAutoCommit(true); }
        }
        return result;
    }

    private static void insertRow(Connection c,List<String> row,Map<String,Integer> ix,String id,String name,int rowNumber)throws SQLException,IOException {
        String local=get(row,ix,"local_name_tamil"), category=get(row,ix,"category"), prep=get(row,ix,"preparation");
        String basis=get(row,ix,"basis"), source=get(row,ix,"data_source"), sourceId=get(row,ix,"source_record_id");
        String status=get(row,ix,"data_status"), citation=get(row,ix,"citation"), license=get(row,ix,"license_note");
        String[] nutrients={"potassium_mg","phosphorus_mg","sodium_mg"};
        double[] values=new double[3];
        for(int i=0;i<3;i++) values[i]=numeric(row,ix,nutrients[i],rowNumber);
        try(PreparedStatement p=c.prepareStatement("INSERT INTO demo_imported_foods(demo_id,food_name) VALUES(?,?)")){
            p.setString(1,id);p.setString(2,name);p.executeUpdate();
        }
        try(PreparedStatement p=c.prepareStatement("INSERT INTO demo_food_metadata(demo_id,food_name,local_name,category,preparation,basis,energy_kcal,protein_g,carbohydrates_g,fat_g,fibre_g,potassium_mg,phosphorus_mg,sodium_mg,calcium_mg,magnesium_mg,iron_mg,source_name,source_record_id,data_status,citation,license_note) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")){
            p.setString(1,id);p.setString(2,name);p.setString(3,nullableText(local));p.setString(4,nullableText(category));p.setString(5,nullableText(prep));p.setString(6,nullableText(basis));
            String[] other={"energy_kcal","protein_g","carbohydrates_g","fat_g","fibre_g"};
            for(int i=0;i<other.length;i++)setNullable(p,7+i,numeric(row,ix,other[i],rowNumber));
            for(int i=0;i<3;i++)setNullable(p,12+i,values[i]);
            String[] minerals={"calcium_mg","magnesium_mg","iron_mg"};
            for(int i=0;i<3;i++)setNullable(p,15+i,numeric(row,ix,minerals[i],rowNumber));
            p.setString(18,nullableText(source));p.setString(19,nullableText(sourceId));p.setString(20,nullableText(status));p.setString(21,nullableText(citation));p.setString(22,nullableText(license));p.executeUpdate();
        }
        for(int i=0;i<3;i++){
            try(PreparedStatement p=c.prepareStatement("INSERT INTO food_nutrient_provenance(demo_id,food_name,nutrient,value,unit,source_name,source_record_id,data_status,citation,license_note,preparation,local_name,basis) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")){
                p.setString(1,id);p.setString(2,name);p.setString(3,nutrients[i]);setNullable(p,4,values[i]);p.setString(5,"mg");p.setString(6,source);p.setString(7,sourceId);p.setString(8,status);p.setString(9,citation);p.setString(10,license);p.setString(11,prep);p.setString(12,local);p.setString(13,basis);p.executeUpdate();
            }
        }
    }

    private static String get(List<String> row,Map<String,Integer> ix,String name){Integer i=ix.get(name);return i==null||i>=row.size()?"":row.get(i).trim();}
    private static String nullableText(String s){return s==null||s.isBlank()?null:s.trim();}
    private static double numeric(List<String> row,Map<String,Integer> ix,String column,int rowNumber)throws IOException{
        String raw=get(row,ix,column);
        if(raw.isBlank()||raw.equalsIgnoreCase("null")||raw.equalsIgnoreCase("na")||raw.equalsIgnoreCase("n/a"))return Double.NaN;
        try{double v=Double.parseDouble(raw);if(!Double.isFinite(v)||v<0)throw new NumberFormatException();return v;}
        catch(NumberFormatException e){throw new IOException("CSV row "+rowNumber+", column "+column+": expected a finite non-negative number or blank; got '"+raw+"'.");}
    }
    private static void setNullable(PreparedStatement p,int i,double value)throws SQLException{if(Double.isNaN(value))p.setNull(i,Types.REAL);else p.setDouble(i,value);}
    private static List<List<String>> readCsv(InputStream input)throws IOException{
        String content=new String(input.readAllBytes(),StandardCharsets.UTF_8);
        if(content.startsWith("\uFEFF"))content=content.substring(1);
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder field=new StringBuilder();boolean quoted=false;
        for(int i=0;i<content.length();i++){char ch=content.charAt(i);
            if(ch=='"'){if(quoted&&i+1<content.length()&&content.charAt(i+1)=='"'){field.append('"');i++;}else quoted=!quoted;}
            else if(ch==','&&!quoted){row.add(field.toString());field.setLength(0);}
            else if((ch=='\n'||ch=='\r')&&!quoted){row.add(field.toString());field.setLength(0);rows.add(row);row=new ArrayList<>();if(ch=='\r'&&i+1<content.length()&&content.charAt(i+1)=='\n')i++;}
            else field.append(ch);
        }
        if(quoted)throw new IOException("CSV contains an unclosed quoted field.");
        if(field.length()>0||!row.isEmpty()){row.add(field.toString());rows.add(row);}
        return rows;
    }
    public static class ImportResult {
        public int imported,skipped;
        @Override public String toString(){return "Imported demo records: "+imported+"\nSkipped duplicate IDs: "+skipped;}
    }
}
