package com.rahulshankar.dialysisapp.service;

import java.sql.*;
import java.util.*;

public class DemoFoodProfileService {
    private final String url;
    public DemoFoodProfileService() throws Exception {
        url="jdbc:sqlite:"+java.nio.file.Path.of(System.getProperty("user.home"),".foodnutrition","foodnutrition.db").toAbsolutePath();
    }
    public List<Map<String,String>> findProfiles() throws SQLException {
        List<Map<String,String>> rows=new ArrayList<>();
        String sql="SELECT food_name,local_name,category,preparation,basis,energy_kcal,protein_g,carbohydrates_g,fat_g,fibre_g,potassium_mg,phosphorus_mg,sodium_mg,calcium_mg,magnesium_mg,iron_mg,data_status,source_name,source_record_id,citation,license_note FROM demo_food_metadata ORDER BY food_name";
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement();ResultSet rs=s.executeQuery(sql)){
            ResultSetMetaData md=rs.getMetaData();int n=md.getColumnCount();
            while(rs.next()){Map<String,String> row=new LinkedHashMap<>();for(int i=1;i<=n;i++){Object v=rs.getObject(i);row.put(md.getColumnLabel(i),v==null?"—":String.valueOf(v));}rows.add(row);}
        }
        return rows;
    }
}
