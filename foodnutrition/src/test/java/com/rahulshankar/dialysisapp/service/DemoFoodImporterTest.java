package com.rahulshankar.dialysisapp.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DemoFoodImporterTest {
    @TempDir Path tempDir;

    private static final String HEADER="demo_id,food_name,local_name_tamil,category,preparation,basis,energy_kcal,protein_g,carbohydrates_g,fat_g,fibre_g,potassium_mg,phosphorus_mg,sodium_mg,calcium_mg,magnesium_mg,iron_mg,data_source,source_record_id,data_status,citation,license_note";

    @Test void importsIntoSeparateDemoDatabaseAndProfileViewerReturnsNutrients() throws Exception {
        String oldHome=System.getProperty("user.home");
        System.setProperty("user.home",tempDir.toString());
        try {
            String row="D1,Test Food,தமிழ்,Demo,Boiled,per 100g,10,1,2,3,4,111,222,333,4,5,6,Test Source,ID1,SYNTHETIC,Test citation,Test license";
            var result=new DemoFoodImporter().importCsv(stream(HEADER+"\n"+row+"\n"));
            assertEquals(1,result.imported);
            List<java.util.Map<String,String>> profiles=new DemoFoodProfileService().findProfiles();
            assertEquals(1,profiles.size());
            assertEquals("111.0",profiles.get(0).get("potassium_mg"));
            assertEquals("222.0",profiles.get(0).get("phosphorus_mg"));
            assertEquals("333.0",profiles.get(0).get("sodium_mg"));
            assertFalse(java.nio.file.Files.exists(tempDir.resolve(".foodnutrition").resolve("foodnutrition.db")),
                    "The demo import must not create or modify the calculator database.");
        } finally { if(oldHome==null)System.clearProperty("user.home");else System.setProperty("user.home",oldHome); }
    }

    @Test void duplicateDemoIdIsSkipped() throws Exception {
        String oldHome=System.getProperty("user.home");
        System.setProperty("user.home",tempDir.toString());
        try {
            String row="D1,Test Food,தமிழ்,Demo,Boiled,per 100g,10,1,2,3,4,111,222,333,4,5,6,Test Source,ID1,SYNTHETIC,Test citation,Test license";
            var first=new DemoFoodImporter().importCsv(stream(HEADER+"\n"+row+"\n"));
            var second=new DemoFoodImporter().importCsv(stream(HEADER+"\n"+row+"\n"));
            assertEquals(1,first.imported);
            assertEquals(1,second.skipped);
            assertEquals(0,second.imported);
            assertEquals(1,new DemoFoodProfileService().findProfiles().size());
        } finally { if(oldHome==null)System.clearProperty("user.home");else System.setProperty("user.home",oldHome); }
    }

    private static ByteArrayInputStream stream(String s){return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));}
}
