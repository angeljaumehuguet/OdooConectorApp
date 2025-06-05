package com.example.odooconectorapp;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4; // Importante

import org.junit.Test; // Necesita junit:junit
import org.junit.runner.RunWith; // Necesita junit:junit

import static org.junit.Assert.*; // Necesita junit:junit

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.odooconectorapp", appContext.getPackageName());
    }
}