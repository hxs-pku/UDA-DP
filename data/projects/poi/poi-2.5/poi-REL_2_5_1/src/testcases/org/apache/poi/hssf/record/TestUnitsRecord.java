
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
 * Tests the serialization and deserialization of the UnitsRecord
 * class works correctly.  Test data taken directly from a real
 * Excel file.
 *

 * @author Glen Stampoultzis (glens at apache.org)
 */
public class TestUnitsRecord
        extends TestCase
{
    byte[] data = new byte[] {
        (byte)0x00, (byte)0x00
    };

    public TestUnitsRecord(String name)
    {
        super(name);
    }

    public void testLoad()
            throws Exception
    {

        UnitsRecord record = new UnitsRecord((short)0x1001, (short)data.length, data);
        assertEquals( 0, record.getUnits());


        assertEquals( 6, record.getRecordSize() );

        record.validateSid((short)0x1001);
    }

    public void testStore()
    {
        UnitsRecord record = new UnitsRecord();
        record.setUnits( (short)0 );


        byte [] recordBytes = record.serialize();
        assertEquals(recordBytes.length - 4, data.length);
        for (int i = 0; i < data.length; i++)
            assertEquals("At offset " + i, data[i], recordBytes[i+4]);
    }
}
