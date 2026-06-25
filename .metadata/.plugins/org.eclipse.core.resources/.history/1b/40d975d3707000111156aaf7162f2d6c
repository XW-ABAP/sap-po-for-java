package com.company.mapping;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

import com.sap.aii.mapping.api.AbstractTrace;
import com.sap.aii.mapping.api.AbstractTransformation;
import com.sap.aii.mapping.api.DynamicConfiguration;
import com.sap.aii.mapping.api.DynamicConfigurationKey;
import com.sap.aii.mapping.api.StreamTransformationException;
import com.sap.aii.mapping.api.TransformationInput;
import com.sap.aii.mapping.api.TransformationOutput;

public class JM_DynamicConfigFileName extends AbstractTransformation{
	
	public static AbstractTrace trace;
	
	public DynamicConfiguration DynConfig;
	
	public void transform(TransformationInput arg0, TransformationOutput arg1) throws StreamTransformationException {
		
		String Filename = "";
		
		DynConfig = arg0.getDynamicConfiguration();
		
		trace = (AbstractTrace) getTrace();
		
		if(DynConfig == null) {
			throw new StreamTransformationException("Unable to find dynamic configuration");
		}
		
		DynamicConfigurationKey key = DynamicConfigurationKey.create("http://sap.com/xi/XI/System/File", Filename);
		
		try {		
			
			Filename = DynConfig.get(key);
			
			if(Filename != null && !Filename.equals("")) {
				trace.addInfo("Filename found from Dynamic Configuration: " + Filename);
			}else {
				trace.addInfo("Filename not found in Dynamic Configuration");
			}
			
		}catch(Exception ee) {
			throw new StreamTransformationException("Unable to Read the Dynamic Configuration for File Parameter");
		}
		
		try {		
			execute(arg0.getInputPayload().getInputStream(), arg1.getOutputPayload().getOutputStream());
		}catch(Exception ee) {
			throw new StreamTransformationException(ee.toString());
		}
	}
	
	public void execute(InputStream in, OutputStream out) throws StreamTransformationException{
		try {
			
			
			StringBuffer sourcePayload = new StringBuffer();
			BufferedReader docInput = new BufferedReader(new InputStreamReader(in));			
			
			String inputLine = "";
			
			
			while( (inputLine = docInput.readLine() ) != null ) {
				sourcePayload.append(inputLine + "\r\n");
			}
			
			out.write( new String(sourcePayload).getBytes("UTF-8") );
			out.flush();
			
		}catch(Exception ee) {
			throw new StreamTransformationException(ee.toString());
		}
		
	}

	public static void main(String[] args) {
		try {
			FileInputStream fin = new FileInputStream("inputfile.txt");
			FileOutputStream fout = new FileOutputStream("outputfile.txt");			
			
			JM_DynamicConfigFileName instance = new JM_DynamicConfigFileName();
			instance.execute(fin, fout);
			
		}catch(Exception ee) {
			ee.printStackTrace();
		}

	}

	@Override
	public Map<String, Object> getSharedInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSharedInstance(Map<String, Object> arg0) {
		// TODO Auto-generated method stub
		
	}

}