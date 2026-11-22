package com.rahulshankar.dialysisapp.service;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class DemoFoodImporter {
    private final String jdbcUrl;
    public DemoFoodImporter() throws Exception {
        Path db=Path.of(System.getProperty("user.home"),".foodnutrition","foodnutrition.db");
        jdbcUrl="jdbc:sqlite:"+db.toAbsolutePath();
    }
    public ImportResult importCsv(InputStream input) throws Exception {
        if(input==null) throw new FileNotFoundException("Demo dataset resource not found.");
        ImportResult result=new ImportResult();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8));
            Connection c=DriverManager.getConnection(jdbcUrl)) {
            c.setAutoCommit(false);
            try {
                try(Statement s=c.createStatement()){
                    s.execute("CREATE TABLE IF NOT EXISTS demo_imported_foods (demo_id TEXT PRIMARY KEY, food_name TEXT NOT NULL, imported_at TEXT DEFAULT CURRENT_TIMESTAMP)");
                    s.execute("CREATE TABLE IF NOT EXISTS food_nutrient_provenance (demo_id TEXT NOT NULL, food_name TEXT NOT NULL, nutrient TEXT NOT NULL, value REAL, unit TEXT NOT NULL, source_name TEXT, source_record_id TEXT, data_status TEXT, citation TEXT, license_note TEXT, preparation TEXT, local_name TEXT, basis TEXT, PRIMARY KEY(demo_id,nutrient))");
                }
                String line; boolean header=true;
                while((line=reader.readLine())!=null){
                    if(header){header=false;continue;}
                    List<String> row=parseCsv(line);
                    if(row.size()<22){result.skipped++;continue;}
                    String id=row.get(0), name=row.get(1), local=row.get(2), category=row.get(3), prep=row.get(4);
                    if(id.isBlank()||name.isBlank()){result.skipped++;continue;}
                    try(PreparedStatement check=c.prepareStatement("SELECT 1 FROM demo_imported_foods WHERE demo_id=?")){check.setString(1,id);try(ResultSet rs=check.executeQuery()){if(rs.next()){result.skipped++;continue;}}}
                    // Preserve existing manually entered catalogue values; only insert absent foods.
                    try(PreparedStatement p=c.prepareStatement("INSERT OR IGNORE INTO food_items(name,category,potassium_mg,phosphorus_mg,sodium_mg) VALUES(?,?,?,?,?)")){
                        p.setString(1,name);p.setString(2,category);setNum(p,3,row.get(11));setNum(p,4,row.get(12));setNum(p,5,row.get(13));p.executeUpdate();
                    }
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO demo_imported_foods(demo_id,food_name) VALUES(?,?)")){p.setString(1,id);p.setString(2,name);p.executeUpdate();}
                    String[] keys={"energy_kcal","protein_g","carbohydrates_g","fat_g","fibre_g","potassium_mg","phosphorus_mg","sodium_mg","calcium_mg","magnesium_mg","iron_mg"};
                    String[] units={"kcal","g","g","g","g","mg","mg","mg","mg","mg","mg"};
                    for(int i=0;i<keys.length;i++){
                        String value=row.get(6+i);
                        try(PreparedStatement p=c.prepareStatement("INSERT INTO food_nutrient_provenance(demo_id,food_name,nutrient,value,unit,source_name,source_record_id,data_status,citation,license_note,preparation,local_name,basis) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")){
                            p.setString(1,id);p.setString(2,name);p.setString(3,keys[i]);setNum(p,4,value);p.setString(5,units[i]);p.setString(6,row.get(17));p.setString(7,row.get(18));p.setString(8,row.get(19));p.setString(9,row.get(20));p.setString(10,row.get(21));p.setString(11,prep);p.setString(12,local);p.setString(13,row.get(5));p.executeUpdate();
                        }
                    }
                    result.imported++;
                }
                c.commit();
            }catch(Exception ex){c.rollback();throw ex;}finally{c.setAutoCommit(true);}
        }
        return result;
    }
    private static void setNum(PreparedStatement p,int index,String v)throws SQLException{if(v==null||v.isBlank())p.setNull(index,Types.REAL);else p.setDouble(index,Double.parseDouble(v));}
    private static List<String> parseCsv(String line){
        List<String> out=new ArrayList<>();StringBuilder b=new StringBuilder();boolean q=false;
        for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='"'){if(q&&i+1<line.length()&&line.charAt(i+1)=='"'){b.append('"');i++;}else q=!q;}else if(ch==','&&!q){out.add(b.toString());b.setLength(0);}else b.append(ch);}out.add(b.toString());return out;
    }
    public static class ImportResult{public int imported,skipped;public String toString(){return "Imported: "+imported+"\\nSkipped (already imported/invalid): "+skipped;}}
}
