
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
        


package org.apache.poi.hssf.record;


import junit.framework.TestCase;

/**
 * Tests the serialization and deserialization of the AxisOptionsRecord
 * class works correctly.  Test data taken directly from a real
 * Excel file.
 *

 * @author Andrew C. Oliver(acoliver at apache.org)
 */
public class TestAxisOptionsRecord
        extends TestCase
{
    byte[] data = new byte[] {        
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x00,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        (byte)0x00,(byte)0xEF,(byte)0x00
    };

    public TestAxisOptionsRecord(String name)
    {
        super(name);
    }

    public void testLoad()
            throws Exception
    {
        AxisOptionsRecord record = new AxisOptionsRecord((short)0x1062, (short)data.length, data);
        assertEquals( 0, record.getMinimumCategory());
        assertEquals( 0, record.getMaximumCategory());
        assertEquals( 1, record.getMajorUnitValue());
        assertEquals( 0, record.getMajorUnit());
        assertEquals( 1, record.getMinorUnitValue());
        assertEquals( 0, record.getMinorUnit());
        assertEquals( 0, record.getBaseUnit());
        assertEquals( 0, record.getCrossingPoint());
        assertEquals( 239, record.getOptions());
        assertEquals( true, record.isDefaultMinimum() );
        assertEquals( true, record.isDefaultMaximum() );
        assertEquals( true, record.isDefaultMajor() );
        assertEquals( true, record.isDefaultMinorUnit() );
        assertEquals( false, record.isIsDate() );
        assertEquals( true, record.isDefaultBase() );
        assertEquals( true, record.isDefaultCross() );
        assertEquals( true, record.isDefaultDateSettings() );


        assertEquals( 22, record.getRecordSize() );

        record.validateSid((short)0x1062);
    }

    public void testStore()
    {
        AxisOptionsRecord record = new AxisOptionsRecord();
        record.setMinimumCategory( (short)0 );
        record.setMaximumCategory( (short)0 );
        record.setMajorUnitValue( (short)1 );
        record.setMajorUnit( (short)0 );
        record.setMinorUnitValue( (short)1 );
        record.setMinorUnit( (short)0 );
        record.setBaseUnit( (short)0 );
        record.setCrossingPoint( (short)0 );
        record.setOptions( (short)239 );
        record.setDefaultMinimum( true );
        record.setDefaultMaximum( true );
        record.setDefaultMajor( true );
        record.setDefaultMinorUnit( true );
        record.setIsDate( false );
        record.setDefaultBase( true );
        record.setDefaultCross( true );
        record.setDefaultDateSettings( true );


        byte [] recordBytes = record.serialize();
        assertEquals(recordBytes.length - 4, data.length);
        for (int i = 0; i < data.length; i++)
            assertEquals("At offset " + i, data[i], recordBytes[i+4]);
    }

    /**
     *  The main program for the TestAxisOptionsRecord class
     *
     *@param  args  The command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Testing org.apache.poi.hssf.record.AxisOptionsRecord");
        junit.textui.TestRunner.run(TestAxisOptionsRecord.class);
    }
}
