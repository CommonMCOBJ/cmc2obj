package org.jmc;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.jmc.util.Log;
import org.jmc.util.Xml;
import org.w3c.dom.Document;
import kotlin.Any
import kotlin.synchronized


/**
 * Version constants.
 */
class Version
{
	companion object {
		private val syncobj: Any = Any();

		private fun initialize()
		{	
			synchronized(syncobj)
			{
				if(rev!=null && dateval!=null && commit!=null) return;

				try {
					val sdf: SimpleDateFormat = SimpleDateFormat("yyyyMMdd hhmm");

					val stream: InputStream = Version::class.java.getClassLoader().getResourceAsStream("data/version.xml");


					val doc: Document = Xml.loadDocument(stream)
					val xpath: XPath = XPathFactory.newInstance().newXPath()

					rev = Integer.valueOf(xpath.evaluate("version/revision", doc, XPathConstants.STRING) as String)
					commit = xpath.evaluate("version/commit", doc, XPathConstants.STRING) as String
					var datestr: String? = xpath.evaluate("version/date", doc, XPathConstants.STRING) as? String

					if (datestr==null) {
						datestr="";
					}

					dateval=sdf.parse(datestr);
				} catch (e: Exception) {
					Log.error("Cannot load program version 2", e, false);
					dateval = Date(0);
					commit = "Unknown";
					rev = 0;
				}
			}
		}


		private var rev: Int? = null;
		@JvmStatic fun VERSION(): Int?
		{
			initialize()
			return rev
		}
		
		private var commit: String? = null;
		@JvmStatic fun COMMIT(): String?
		{
			initialize()
			return commit
		}

		private var dateval: Date? = null;
		@JvmStatic fun DATE(): Date?
		{
			initialize()
			return dateval
		}
	}
}
