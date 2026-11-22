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
    private static final String ROW="D1,Test Food,தமிழ்,Demo,Boiled,per 100g,10,1,2,3,4,111,222,333,4,5,6,Test Source,ID1,SYNTHETIC,Test citation,Test license";

    @Test void importsIntoSeparateDemoDatabaseAndViewerReturnsMinerals() throws Exception {
        withHome(() -> {
            var result=new DemoFoodImporter().importCsv(stream(HEADER+"\\n"+ROW+"\\n"));
            assertEquals(1,result.imported);
            List<java.util.Map<String,String>> profiles=new DemoFoodProfileService().findProfiles();
            assertEquals(1,profiles.size());
            assertEquals("111.0",profiles.get(0).get("potassium_mg"));
            assertEquals("222.0",profiles.get(0).get("phosphorus_mg"));
            assertEquals("333.0",profiles.get(0).get("sodium_mg"));
            assertFalse(java.nio.file.Files.exists(tempDir.resolve(".foodnutrition").resolve("foodnutrition.db")),
                    "Demo import must not create or modify the calculator database.");
        });
    }

    @Test void duplicateIdIsSkipped() throws Exception {
        withHome(() -> {
            var first=new DemoFoodImporter().importCsv(stream(HEADER+"\\n"+ROW+"\\n"));
            var second=new DemoFoodImporter().importCsv(stream(HEADER+"\\n"+ROW+"\\n"));
            assertEquals(1,first.imported);assertEquals(1,second.skipped);assertEquals(0,second.imported);
            assertEquals(1,new DemoFoodProfileService().findProfiles().size());
        });
    }

    @Test void malformedRowRollsBackEntireImport() throws Exception {
        withHome(() -> {
            String bad=ROW.replace("D1,","D2,").replace(",111,222,333,",",not-a-number,222,333,");
            assertThrows(Exception.class,()->new DemoFoodImporter().importCsv(stream(HEADER+"\\n"+ROW+"\\n"+bad+"\\n")));
            assertTrue(new DemoFoodProfileService().findProfiles().isEmpty(),
                    "A failed batch should leave no partially imported profiles.");
        });
    }

    @Test void acceptsMultilineQuotedTextAndBlankUnknownNutrients() throws Exception {
        withHome(() -> {
            String row=ROW.replace("Test citation","\\\"Citation line 1\\nCitation line 2\\\"").replace(",111,222,333,",",,222,333,");
            new DemoFoodImporter().importCsv(stream(HEADER+"\\n"+row+"\\n"));
            var profile=new DemoFoodProfileService().findProfiles().get(0);
            assertEquals("—",profile.get("potassium_mg"));
            assertEquals("222.0",profile.get("phosphorus_mg"));
            assertTrue(profile.get("citation").contains("Citation line 2"));
        });
    }

    @Test void missingHeaderIsRejected() throws Exception {
        withHome(() -> assertThrows(Exception.class,
                ()->new DemoFoodImporter().importCsv(stream("demo_id,food_name\\nD1,Food\\n"))));
    }

    @Test void deleteAllRemovesOnlyDemoProfiles() throws Exception {
        withHome(() -> {
            new DemoFoodImporter().importCsv(stream(HEADER+"\\n"+ROW+"\\n"));
            assertEquals(1,new DemoFoodProfileService().deleteAllDemoProfiles());
            assertTrue(new DemoFoodProfileService().findProfiles().isEmpty());
            assertFalse(java.nio.file.Files.exists(tempDir.resolve(".foodnutrition").resolve("foodnutrition.db")));
        });
    }

    private void withHome(ThrowingRunnable test) throws Exception {
        String oldHome=System.getProperty("user.home");
        System.setProperty("user.home",tempDir.toString());
        try { test.run(); }
        finally { if(oldHome==null)System.clearProperty("user.home");else System.setProperty("user.home",oldHome); }
    }
    private static ByteArrayInputStream stream(String s){return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));}
    @FunctionalInterface private interface ThrowingRunnable {void run() throws Exception;}
}
