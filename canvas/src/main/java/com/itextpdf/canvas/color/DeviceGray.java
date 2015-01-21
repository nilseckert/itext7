package com.itextpdf.canvas.color;

import com.itextpdf.core.pdf.colorspace.PdfDeviceCs;

public class DeviceGray extends Color {

    static final public DeviceGray White = new DeviceGray(1f);
    static final public DeviceGray Black = new DeviceGray();

    public DeviceGray(float value) {
        super(new PdfDeviceCs.Gray(), new float[] {value});
    }

    public DeviceGray() {
        this(0);
    }

}