package haven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ModelDiscovery {
    private static final boolean enabled = Boolean.getBoolean("kami.modelscan");
    private static final Set<String> seen = Collections.synchronizedSet(new HashSet<String>());

    public static void loaded(Resource res) {
	if(!enabled || (res == null))
	    return;

	int meshes = 0;
	for(FastMesh.MeshRes ignored : res.layers(FastMesh.MeshRes.class))
	    meshes++;

	if(meshes <= 0)
	    return;

	String line = String.format("%s v%d meshes=%d", res.name, res.ver, meshes);
	if(seen.add(line))
	    append(line);
    }

    private static void append(String line) {
	File out = Config.getFile("model-scan.txt");
	try(PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out, true), "UTF-8"))) {
	    pw.println(line);
	} catch(IOException e) {
	    e.printStackTrace();
	}
    }
}
