
/* ====================================================================
   Copyright 2002-2004   Apache Software Foundation

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
        

package org.apache.poi.hssf.usermodel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import junit.framework.TestCase;

import org.apache.poi.hssf.model.Sheet;
import org.apache.poi.hssf.record.HCenterRecord;
import org.apache.poi.hssf.record.ProtectRecord;
import org.apache.poi.hssf.record.SCLRecord;
import org.apache.poi.hssf.record.VCenterRecord;
import org.apache.poi.hssf.record.WSBoolRecord;
import org.apache.poi.hssf.record.WindowTwoRecord;
import org.apache.poi.hssf.util.Region;

/**
 * Tests HSSFSheet.  This test case is very incomplete at the moment.
 *
 *
 * @author Glen Stampoultzis (glens at apache.org)
 */

public class TestHSSFSheet
        extends TestCase
{
    public TestHSSFSheet(String s)
    {
        super(s);
    }

    /**
     * Test the gridset field gets set as expected.
     */

    public void testBackupRecord()
            throws Exception
    {
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet s = wb.createSheet();
        Sheet sheet = s.getSheet();

        assertEquals(true, sheet.getGridsetRecord().getGridset());
        s.setGridsPrinted(true);
        assertEquals(false, sheet.getGridsetRecord().getGridset());
    }

    /**
     * Test vertically centered output.
     */

    public void testVerticallyCenter()
            throws Exception
    {
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet s = wb.createSheet();
        Sheet sheet = s.getSheet();
        VCenterRecord record =
                (VCenterRecord) sheet.findFirstRecordBySid(VCenterRecord.sid);

        assertEquals(false, record.getVCenter());
        s.setVerticallyCenter(true);
        assertEquals(true, record.getVCenter());

        // wb.write(new FileOutputStream("c:\\test.xls"));
    }

    /**
     * Test horizontally centered output.
     */

    public void testHorizontallyCenter()
            throws Exception
    {
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet s = wb.createSheet();
        Sheet sheet = s.getSheet();
        HCenterRecord record =
                (HCenterRecord) sheet.findFirstRecordBySid(HCenterRecord.sid);

        assertEquals(false, record.getHCenter());
        s.setHorizontallyCenter(true);
        assertEquals(true, record.getHCenter());

    }    
    
    
    /**
     * Test WSBboolRecord fields get set in the user model.
     */

    public void testWSBool()
    {
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet s = wb.createSheet();
        Sheet sheet = s.getSheet();
        WSBoolRecord record =
                (WSBoolRecord) sheet.findFirstRecordBySid(WSBoolRecord.sid);

        // Check defaults
        assertEquals(true, record.getAlternateExpression());
        assertEquals(true, record.getAlternateFormula());
        assertEquals(false, record.getAutobreaks());
        assertEquals(false, record.getDialog());
        assertEquals(false, record.getDisplayGuts());
        assertEquals(true, record.getFitToPage());
        assertEquals(false, record.getRowSumsBelow());
        assertEquals(false, record.getRowSumsRight());

        // Alter
        s.setAlternativeExpression(false);
        s.setAlternativeFormula(false);
        s.setAutobreaks(true);
        s.setDialog(true);
        s.setDisplayGuts(true);
        s.setFitToPage(false);
        s.setRowSumsBelow(true);
        s.setRowSumsRight(true);

        // Check
        assertEquals(false, record.getAlternateExpression());
        assertEquals(false, record.getAlternateFormula());
        assertEquals(true, record.getAutobreaks());
        assertEquals(true, record.getDialog());
        assertEquals(true, record.getDisplayGuts());
        assertEquals(false, record.getFitToPage());
        assertEquals(true, record.getRowSumsBelow());
        assertEquals(true, record.getRowSumsRight());
        assertEquals(false, s.getAlternateExpression());
        assertEquals(false, s.getAlternateFormula());
        assertEquals(true, s.getAutobreaks());
        assertEquals(true, s.getDialog());
        assertEquals(true, s.getDisplayGuts());
        assertEquals(false, s.getFitToPage());
        assertEquals(true, s.getRowSumsBelow());
        assertEquals(true, s.getRowSumsRight());
    }

    public void testReadBooleans()
            throws Exception
    {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Test boolean");
        HSSFRow row = sheet.createRow((short) 2);
        HSSFCell cell = row.createCell((short) 9);
        cell.setCellValue(true);
        cell = row.createCell((short) 11);
        cell.setCellValue(true);
        File tempFile = File.createTempFile("bool", "test.xls");
        FileOutputStream stream = new FileOutputStream(tempFile);
        workbook.write(stream);
        stream.close();

        FileInputStream readStream = new FileInputStream(tempFile);
        workbook = new HSSFWorkbook(readStream);
        sheet = workbook.getSheetAt(0);
        row = sheet.getRow(2);
        stream.close();
        tempFile.delete();
        assertNotNull(row);
        assertEquals(2, row.getPhysicalNumberOfCells());
    }

    public void testRemoveRow()
    {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Test boolean");
        HSSFRow row = sheet.createRow((short) 2);
        sheet.removeRow(row);
    }

    public void testCloneSheet() {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Test Clone");
        HSSFRow row = sheet.createRow((short) 0);
        HSSFCell cell = row.createCell((short) 0);
        cell.setCellValue("clone_test"); 
        HSSFSheet cloned = workbook.cloneSheet(0);
  
        //Check for a good clone
        assertEquals(cloned.getRow((short)0).getCell((short)0).getStringCellValue(), "clone_test");
        
        //Check that the cells are not somehow linked
        cell.setCellValue("Difference Check");
        assertEquals(cloned.getRow((short)0).getCell((short)0).getStringCellValue(), "clone_test");
    }
    
	/**
	 * Test that the ProtectRecord is included when creating or cloning a sheet
	 */
	public void testProtect() {
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet hssfSheet = workbook.createSheet();
		Sheet sheet = hssfSheet.getSheet();
		ProtectRecord protect = sheet.getProtect();
   	
		assertFalse(protect.getProtect());

		// This will tell us that cloneSheet, and by extension,
		// the list forms of createSheet leave us with an accessible
		// ProtectRecord.
		hssfSheet.setProtect(true);
		Sheet cloned = sheet.cloneSheet();
		assertNotNull(cloned.getProtect());
		assertTrue(hssfSheet.getProtect());
	}


    public void testZoom()
            throws Exception
    {
        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet = wb.createSheet();
        assertEquals(-1, sheet.getSheet().findFirstRecordLocBySid(SCLRecord.sid));
        sheet.setZoom(3,4);
        assertTrue(sheet.getSheet().findFirstRecordLocBySid(SCLRecord.sid) > 0);
        SCLRecord sclRecord = (SCLRecord) sheet.getSheet().findFirstRecordBySid(SCLRecord.sid);
        assertEquals(3, sclRecord.getNumerator());
        assertEquals(4, sclRecord.getDenominator());

        int sclLoc = sheet.getSheet().findFirstRecordLocBySid(SCLRecord.sid);
        int window2Loc = sheet.getSheet().findFirstRecordLocBySid(WindowTwoRecord.sid);
        assertTrue(sclLoc == window2Loc + 1);

    }
    

	/**
	 * When removing one merged region, it would break
	 *
	 */    
	public void testRemoveMerged() {
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = wb.createSheet();
		Region region = new Region(0, (short)0, 1, (short)1);   	
		sheet.addMergedRegion(region);
		region = new Region(1, (short)0, 2, (short)1);
		sheet.addMergedRegion(region);
		
    	sheet.removeMergedRegion(0);
    	
    	region = sheet.getMergedRegionAt(0);
    	assertEquals("Left over region should be starting at row 1", 1, region.getRowFrom());
    	
    	sheet.removeMergedRegion(0);
    	
		assertEquals("there should be no merged regions left!", 0, sheet.getNumMergedRegions());
		
		//an, add, remove, get(0) would null pointer
		sheet.addMergedRegion(region);
		assertEquals("there should now be one merged region!", 1, sheet.getNumMergedRegions());
		sheet.removeMergedRegion(0);
		assertEquals("there should now be zero merged regions!", 0, sheet.getNumMergedRegions());
		//add it again!
		region.setRowTo(4);
    	
		sheet.addMergedRegion(region);
		assertEquals("there should now be one merged region!", 1, sheet.getNumMergedRegions());
		
		//should exist now!
		assertTrue("there isn't more than one merged region in there", 1 <= sheet.getNumMergedRegions());
		region = sheet.getMergedRegionAt(0);
		assertEquals("the merged row to doesnt match the one we put in ", 4, region.getRowTo());
    	
    }

	public void testShiftMerged() {
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = wb.createSheet();
		HSSFRow row = sheet.createRow(0);
		HSSFCell cell = row.createCell((short)0);
		cell.setCellValue("first row, first cell");
		
		row = sheet.createRow(1);
		cell = row.createCell((short)1);
		cell.setCellValue("second row, second cell");
		
		Region region = new Region(1, (short)0, 1, (short)1);   	
		sheet.addMergedRegion(region);
		
		sheet.shiftRows(1, 1, 1);
		
		region = sheet.getMergedRegionAt(0);
		assertEquals("Merged region not moved over to row 2", 2, region.getRowFrom());
		
	}

    /**
     * Tests the display of gridlines, formulas, and rowcolheadings.
     * @author Shawn Laubach (slaubach at apache dot org)
     */
    public void testDisplayOptions() throws Exception {
    	HSSFWorkbook wb = new HSSFWorkbook();
    	HSSFSheet sheet = wb.createSheet();
	
        File tempFile = File.createTempFile("display", "test.xls");
        FileOutputStream stream = new FileOutputStream(tempFile);
        wb.write(stream);
        stream.close();

        FileInputStream readStream = new FileInputStream(tempFile);
        wb = new HSSFWorkbook(readStream);
        sheet = wb.getSheetAt(0);
    	readStream.close();
    
    	assertEquals(sheet.isDisplayGridlines(), true);
    	assertEquals(sheet.isDisplayRowColHeadings(), true);
    	assertEquals(sheet.isDisplayFormulas(), false);
    
    	sheet.setDisplayGridlines(false);
    	sheet.setDisplayRowColHeadings(false);
    	sheet.setDisplayFormulas(true);

        tempFile = File.createTempFile("display", "test.xls");
        stream = new FileOutputStream(tempFile);
        wb.write(stream);
        stream.close();

        readStream = new FileInputStream(tempFile);
        wb = new HSSFWorkbook(readStream);
        sheet = wb.getSheetAt(0);
    	readStream.close();
    
    
    	assertEquals(sheet.isDisplayGridlines(), false);
    	assertEquals(sheet.isDisplayRowColHeadings(), false);
    	assertEquals(sheet.isDisplayFormulas(), true);
    }

    
    /**
     * Make sure the excel file loads work
     *
     */
    public void testPageBreakFiles() throws Exception{
        FileInputStream fis = null;
        HSSFWorkbook wb     = null;
        
        String filename = System.getProperty("HSSF.testdata.path");

        filename = filename + "/SimpleWithPageBreaks.xls";
        fis = new FileInputStream(filename);
        wb = new HSSFWorkbook(fis);
        fis.close();
        
        HSSFSheet sheet = wb.getSheetAt(0);
        assertNotNull(sheet);
        
        assertEquals("1 row page break", 1, sheet.getRowBreaks().length);
        assertEquals("1 column page break", 1, sheet.getColumnBreaks().length);

        assertTrue("No row page break", sheet.isRowBroken(22));
        assertTrue("No column page break", sheet.isColumnBroken((short)4));
        
        sheet.setRowBreak(10);        
        sheet.setColumnBreak((short)13);

        assertEquals("row breaks number", 2, sheet.getRowBreaks().length);
        assertEquals("column breaks number", 2, sheet.getColumnBreaks().length);
        
        File tempFile = File.createTempFile("display", "testPagebreaks.xls");
        FileOutputStream stream = new FileOutputStream(tempFile);
        wb.write(stream);
        stream.close();
        
        wb = new HSSFWorkbook(new FileInputStream(tempFile));
        sheet = wb.getSheetAt(0);

        assertTrue("No row page break", sheet.isRowBroken(22));
        assertTrue("No column page break", sheet.isColumnBroken((short)4));

        
        assertEquals("row breaks number", 2, sheet.getRowBreaks().length);
        assertEquals("column breaks number", 2, sheet.getColumnBreaks().length);
        
        
    }
    
	public static void main(java.lang.String[] args) {
		 junit.textui.TestRunner.run(TestHSSFSheet.class);
	}    
}
