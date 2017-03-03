package de.julielab.gepi.core.services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.tapestry5.ioc.services.SymbolProvider;

public class FileSymbolProvider implements SymbolProvider {

	private Properties properties;

	public FileSymbolProvider(String path) throws FileNotFoundException, IOException {
		this.properties = new Properties();
		this.properties.load(new FileInputStream(path));
	}

	@Override
	public String valueForSymbol(String symbolName) {
		return properties.getProperty(symbolName);
	}

}
