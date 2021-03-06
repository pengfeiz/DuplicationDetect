package com.dafei.datasource;

import com.dafei.bean.MetaData;
import com.dafei.bean.Term;
import com.dafei.config.ConnectionFactory;
import com.dafei.config.DBConn;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: dafei
 * Date: 11-3-4
 * Time: 2:38pm
 * To change this template use File | Settings | File Templates.
 */
// -Xmx 1024m
public class Dump2HbaseMain {

    public static void main(String args[]) throws Exception {
        Dump2HbaseMain d2h = new Dump2HbaseMain();
        //d2h.dumptoHbase();
        //  d2h.getMDTest();
        // d2h.getTTest();
        // d2h.scanMDTest();
        d2h.scanTermTest();
    }

    private static Log log = LogFactory.getLog(Dump2HbaseMain.class);
    private final static HBaseConfiguration mConfig = new HBaseConfiguration();

    static {
        mConfig.addResource(new Path(ConnectionFactory.getInstance().getDefaultConfig()));
        mConfig.addResource(new Path(ConnectionFactory.getInstance().getSiteConfig()));
    }

    public void getTTest() throws Exception {
        HTable t = new HTable(mConfig, "term");
        Get g = new Get(Bytes.toBytes("805539"));
        Result r = t.get(g);
        byte value1[] = r.getValue(Bytes.toBytes("data"), Bytes.toBytes("tname"));
        byte value2[] = r.getValue(Bytes.toBytes("metadata"), Bytes.toBytes("5368"));
        String s1 = Bytes.toString(value1);
        String s2 = Bytes.toString(value2);
        System.out.println("get term id=805539 data:tname=" + s1 + ",metadata:5368=" + s2);
        t.close();

    }


    public void getMDTest() throws Exception {
        HTable t = new HTable(mConfig, "metadata");
        Get g = new Get(Bytes.toBytes("5368"));
        Result r = t.get(g);
        byte value[] = r.getValue(Bytes.toBytes("data"), Bytes.toBytes("uri"));
        String category = Bytes.toString(r.getValue(Bytes.toBytes("data"), Bytes.toBytes("cate")));
        byte value2[] = r.getValue(Bytes.toBytes("term"), Bytes.toBytes("805539"));
        String s = Bytes.toString(value);
        System.out.println("get metadata id=5368 data:other_courseware_name=" + category + ",data:uri=" + s + ",term:805539=" + Bytes.toString(value2));
        t.close();
    }

    public void scanMDTest() throws Exception {
        HTable t = new HTable(mConfig, "metadata");
        Scan s = new Scan();

        s.addColumn(Bytes.toBytes("term"), Bytes.toBytes("805539"));
        ResultScanner scanner = t.getScanner(s);
        int i = 0;
        for (Result rr = scanner.next(); rr != null; rr = scanner.next()) {
            System.out.println("Found row:" + rr);
        }
        System.out.println(i);
        scanner.close();
        //t.close();
    }

    public void scanTermTest() throws Exception {
        HTable t = new HTable(mConfig, "term");
        Set<Term> termSet = new HashSet<Term>();
        Scan s = new Scan();
        s.addColumn(Bytes.toBytes("data"));
        ResultScanner scanner = t.getScanner(s);
        for (Result rr = scanner.next(); rr != null; rr = scanner.next()) {
            for (KeyValue kv : rr.raw()) {
                System.out.println(Bytes.toString(kv.getRow()) + " " + Bytes.toString(kv.getColumn()) + " " + Bytes.toString(kv.getValue()));
                termSet.add(new Term(Integer.parseInt(Bytes.toString(kv.getRow())), Bytes.toString(kv.getValue())));
            }

        }
        System.out.println(termSet.size());
        scanner.close();
        t.close();
    }

    public List<Put> saveTermlist(Term t, HTable table) {

        List<Put> puts = new ArrayList<Put>();
        try {

            String s = "select a.re_id as mdid, b.re_temp_location as uri, b.other_courseware_name as category " +
                    "from db2admin.term_and_resource_no_duplication a inner join db2admin.metadata b on a.re_id=b.re_id where t_id=?";
            Put p = new Put(Bytes.toBytes(String.valueOf(t.getT_id())));
            p.add(Bytes.toBytes("data"), Bytes.toBytes("tname"), System.currentTimeMillis(), Bytes.toBytes(t.getT_name()));
            DBConn dbconn = DBConn.getInstance();
            if (dbconn.openDB() == 1) {
                dbconn.prepareStatement(s);
                dbconn.setInt(1, t.getT_id());
                ResultSet rs = dbconn.doQuery();
                int i = 0;
                while (rs.next()) {
                    i++;
                    p.add(Bytes.toBytes("metadata"), Bytes.toBytes(String.valueOf(rs.getInt("mdid"))), System.currentTimeMillis() + i, Bytes.toBytes(rs.getString("uri")));
                    puts.add(p);
                }
            }
            table.put(puts);
            puts = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return puts;
    }

    public List<Put> saveMetadatalist(MetaData md, HTable table) {

        List<Put> puts = new ArrayList<Put>();
        try {
            String s = "select a.t_id as tid, b.t_name as tname " +
                    "from db2admin.term_and_resource_no_duplication a inner join db2admin.terms b on a.t_id=b.t_id where re_id=?";
            Put p = new Put(Bytes.toBytes(String.valueOf(md.getRe_id())));
            p.add(Bytes.toBytes("data"), Bytes.toBytes("uri"), System.currentTimeMillis(), Bytes.toBytes(md.getRe_temp_location()));
            p.add(Bytes.toBytes("data"), Bytes.toBytes("cate"), System.currentTimeMillis(), Bytes.toBytes(md.getOther_courseware_name()));
            p.add(Bytes.toBytes("data"), Bytes.toBytes("courseware_id"), System.currentTimeMillis(), Bytes.toBytes(String.valueOf(md.getCourseware_id())));
            if (null != md.getRe_identify() && !"".equals(md.getRe_identify()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("re_identify"), System.currentTimeMillis(), Bytes.toBytes(md.getRe_identify()));
            if (null != md.getIs_delete() && !"".equals(md.getIs_delete()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("is_delete"), System.currentTimeMillis(), Bytes.toBytes(md.getIs_delete()));
            if (null != md.getIs_auth() && !"".equals(md.getIs_auth()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("is_auth"), System.currentTimeMillis(), Bytes.toBytes(md.getIs_auth()));
            if (null != md.getIs_distribute() && !"".equals(md.getIs_distribute()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("is_distribute"), System.currentTimeMillis(), Bytes.toBytes(md.getIs_distribute()));
            p.add(Bytes.toBytes("data"), Bytes.toBytes("re_size"), System.currentTimeMillis(), Bytes.toBytes(String.valueOf(md.getRe_size())));
            if (null != md.getRe_location() && !"".equals(md.getRe_location()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("re_location"), System.currentTimeMillis(), Bytes.toBytes(md.getRe_location()));
            p.add(Bytes.toBytes("data"), Bytes.toBytes("re_upload_date"), System.currentTimeMillis(), Bytes.toBytes(md.getRe_upload_date().toString()));
            p.add(Bytes.toBytes("data"), Bytes.toBytes("re_upload_userid"), System.currentTimeMillis(), Bytes.toBytes(String.valueOf(md.getRe_upload_userid())));
            p.add(Bytes.toBytes("data"), Bytes.toBytes("subject_id"), System.currentTimeMillis(), Bytes.toBytes(String.valueOf(md.getSubject_id())));
            if (null != md.getMe_object() && !"".equals(md.getMe_object()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_object"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_object()));
            if (null != md.getMe_education() && !"".equals(md.getMe_education()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_education"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_education()));
            if (null != md.getMe_title() && !"".equals(md.getMe_title()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_title"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_title()));
            if (null != md.getMe_author() && !"".equals(md.getMe_author()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_author"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_author()));
            if (null != md.getMe_key() && !"".equals(md.getMe_key()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_key"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_key()));
            if (null != md.getMe_abstract() && !"".equals(md.getMe_abstract()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_abstract"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_abstract()));
            if (null != md.getMe_publisher() && !"".equals(md.getMe_publisher()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_publisher"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_publisher()));
            if (null != md.getMe_pub_date())
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_pub_date"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_pub_date().toString()));
            if (null != md.getMe_language() && !"".equals(md.getMe_language()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_language"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_language()));
            if (null != md.getMe_version() && !"".equals(md.getMe_version()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_version"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_version()));
            if (null != md.getMe_copyright() && !"".equals(md.getMe_copyright()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_copyright"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_copyright()));
            if (null != md.getMe_type() && !"".equals(md.getMe_type()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_type"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_type()));
            if (null != md.getFile_type() && !"".equals(md.getFile_type()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("file_type"), System.currentTimeMillis(), Bytes.toBytes(md.getFile_type()));
            if (null != md.getFile_format() && !"".equals(md.getFile_format()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("file_format"), System.currentTimeMillis(), Bytes.toBytes(md.getFile_format()));
            if (null != md.getMe_source() && !"".equals(md.getMe_source()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_source"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_source()));
            if (null != md.getMe_draw_out() && !"".equals(md.getMe_draw_out()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("me_draw_out"), System.currentTimeMillis(), Bytes.toBytes(md.getMe_draw_out()));
            if (null != md.getXjtudlc_id() && !"".equals(md.getXjtudlc_id()))
                p.add(Bytes.toBytes("data"), Bytes.toBytes("xjtudlc_id"), System.currentTimeMillis(), Bytes.toBytes(md.getXjtudlc_id()));

            DBConn dbconn = DBConn.getInstance();
            if (dbconn.openDB() == 1) {
                dbconn.prepareStatement(s);
                dbconn.setInt(1, md.getRe_id());
                ResultSet rs = dbconn.doQuery();
                int i = 0;
                while (rs.next()) {
                    i++;
                    p.add(Bytes.toBytes("term"), Bytes.toBytes(String.valueOf(rs.getInt("tid"))), System.currentTimeMillis() + i, Bytes.toBytes(rs.getString("tname")));
                    puts.add(p);

                }
            }
            table.put(puts);
            puts = null;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return puts;
    }


    public void dumptoHbase() {
        long startpoint = System.currentTimeMillis();
        System.out.println("start insert to hbase at:" + startpoint);

        try {
            HTable t1 = new HTable(mConfig, "term");
            HTable t2 = new HTable(mConfig, "metadata");
            String s1 = "select * from db2admin.terms";
            String s2 = "select * from db2admin.metadata a where re_id in ( select re_id from db2admin.term_and_resource_no_duplication group by re_id)";
            DBConn dbconn = DBConn.getInstance();
            if (dbconn.openDB() == 1) {
                ResultSet rs1 = dbconn.doQuery(s1);
                while (rs1.next()) {
                    Term t = new Term();
                    t.setT_id(rs1.getInt("t_id"));
                    t.setT_name(rs1.getString("t_name"));
                    System.out.println("saving term " + t.getT_name() + " and its metadatas");
                    saveTermlist(t, t1);

                }
                ResultSet rs2 = dbconn.doQuery(s2);
                while (rs2.next()) {

                    MetaData md = new MetaData();
                    md.setRe_id(rs2.getInt("RE_ID"));
                    md.setCourseware_id(rs2.getInt("COURSEWARE_ID"));
                    md.setOther_courseware_name(rs2.getString("OTHER_COURSEWARE_NAME"));
                    md.setRe_identify(rs2.getString("RE_IDENTIFY"));
                    md.setIs_delete(rs2.getString("IS_DELETE"));
                    md.setIs_auth(rs2.getString("IS_AUTH"));
                    md.setIs_distribute(rs2.getString("IS_DISTRIBUTE"));
                    md.setRe_size(rs2.getInt("RE_SIZE"));
                    md.setRe_location(rs2.getString("RE_LOCATION"));
                    md.setRe_temp_location(rs2.getString("RE_TEMP_LOCATION"));
                    md.setRe_upload_date(rs2.getDate("RE_UPLOAD_DATE"));
                    md.setRe_upload_userid(rs2.getInt("RE_UPLOAD_USERID"));
                    md.setSubject_id(rs2.getInt("SUBJECT_ID"));
                    md.setMe_object(rs2.getString("ME_OBJECT"));
                    md.setMe_education(rs2.getString("ME_EDUCATION"));
                    md.setMe_title(rs2.getString("ME_TITLE"));
                    md.setMe_author(rs2.getString("ME_AUTHOR"));
                    md.setMe_key(rs2.getString("ME_KEY"));
                    md.setMe_abstract(rs2.getString("ME_ABSTRACT"));
                    md.setMe_publisher(rs2.getString("ME_PUBLISHER"));
                    md.setMe_pub_date(rs2.getDate("ME_PUB_DATE"));
                    md.setMe_language(rs2.getString("ME_LANGUAGE"));
                    md.setMe_version(rs2.getString("ME_VERSION"));
                    md.setMe_copyright(rs2.getString("ME_COPYRIGHT"));
                    md.setMe_type(rs2.getString("ME_TYPE"));
                    md.setFile_type(rs2.getString("FILE_TYPE"));
                    md.setFile_format(rs2.getString("FILE_FORMAT"));
                    md.setMe_source(rs2.getString("ME_SOURCE"));
                    md.setMe_draw_out(rs2.getString("ME_DRAW_OUT"));
                    md.setXjtudlc_id(rs2.getString("XJTUDLC_ID"));
                    System.out.println("saving metadata " + md.getRe_temp_location() + " and its terms");
                    if (md.getRe_id() != 940 && md.getRe_id() != 1056)
                        saveMetadatalist(md, t2);

                }
            }


            long endpoint = System.currentTimeMillis();
            System.out.println("end insert,total:" + (endpoint - startpoint) / 1000);
            t1.close();
            t2.close();
            dbconn.closeDB();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
