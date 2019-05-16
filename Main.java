import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.*;
import java.util.*;

public class Main{
	static class Type{
		String name;
		int array;
		Type(mxstarParser.TypeContext ttx){
			name=ttx.getChild(0).getText();
			array=ttx.getChildCount()/2;
		}
		Type(String o){
			name=o;
		}
		boolean equal(Type tmp){
			//System.out.printf(name+"\n"+tmp.name+"\n"+array+"\n"+tmp.array+"\n%b\n",tmp.name==name&&tmp.array==array);
			if(name.equals("void")!=tmp.name.equals("void"))
				return false;
			return tmp.name.equals(name)&&tmp.array==array||
				tmp.name=="null"&&(array!=0||!name.equals("int")&&!name.equals("bool")&&!name.equals("string"))||
				name=="null"&&(tmp.array!=0||!tmp.name.equals("int")&&!tmp.name.equals("bool")&&!tmp.name.equals("string"));
		}
	}
	static class Node{
		String kind;
		String id;
		List<Node>defs;
		Type type;
		List<Type>types;
		List<String>names;
		List<Node>expr;
		List<Node>stmts;
		Map<String,Def>scope;
		boolean isscope;
		boolean leftv;
		List<Code>ir;
		int ans;
		boolean ismem;
		Node(){
			defs=new ArrayList<Node>();
			types=new ArrayList<Type>();
			names=new ArrayList<String>();
			expr=new ArrayList<Node>();
			stmts=new ArrayList<Node>();
			scope=new TreeMap<String,Def>();
			ir=new ArrayList<Code>();
		}
	}
	static int t1,t2;
	static Node visit(ParseTree now)throws Exception{
		for(int i=1;i<=t1;i++)
			System.out.printf("\t");
		t1++;
		System.out.print(now.getText());
		System.out.printf(":");
		//System.out.printf("???\n");
		Node tmp=new Node();
		if(now instanceof mxstarParser.CodeContext){
			System.out.printf("code\n");
			mxstarParser.CodeContext ctx=(mxstarParser.CodeContext)now;
			tmp.kind="code";
			tmp.isscope=true;
			for(ParseTree i:ctx.children)
				tmp.defs.add(visit(i));
		}
		else if(now instanceof mxstarParser.ClassdefContext){
			System.out.printf("classdef\n");
			mxstarParser.ClassdefContext ctx=(mxstarParser.ClassdefContext)now;
			tmp.kind="classdef";
			tmp.isscope=true;
			tmp.id=ctx.Name().getText();
			for(int i=3;i+1<ctx.getChildCount();i++)
				tmp.defs.add(visit(ctx.getChild(i)));
		}
		else if(now instanceof mxstarParser.FuncdefContext){
			System.out.printf("func\n");
			mxstarParser.FuncdefContext ctx=(mxstarParser.FuncdefContext)now;
			tmp.kind="funcdef";
			tmp.isscope=true;
			tmp.id=ctx.Name(0).getText();
			tmp.type=new Type(ctx.type(0));
			for(int i=1;i<ctx.type().size();i++){
				tmp.types.add(new Type(ctx.type(i)));
				tmp.names.add(ctx.Name(i).getText());
			}
			for(ParseTree i:ctx.statement())
				tmp.stmts.add(visit(i));
		}
		else if(now instanceof mxstarParser.ConstructfuncdefContext){
			System.out.printf("construct\n");
			mxstarParser.ConstructfuncdefContext ctx=(mxstarParser.ConstructfuncdefContext)now;
			tmp.kind="construct";
			tmp.isscope=true;
			tmp.id=ctx.Name().getText();
			for(ParseTree i:ctx.statement())
				tmp.stmts.add(visit(i));
		}
		else if(now instanceof mxstarParser.VardefContext){
			System.out.printf("vardef\n");
			mxstarParser.VardefContext ctx=(mxstarParser.VardefContext)now;
			tmp.kind="vardef";
			tmp.type=new Type(ctx.type());
			for(int i=1;i<ctx.getChildCount();i+=2){
				tmp.names.add(ctx.getChild(i).getText());
				if(ctx.getChild(i+1).getText().equals("=")){
					tmp.expr.add(visit(ctx.getChild(i+2)));
					i+=2;
				}
				else tmp.expr.add(null);
			}
		}
		else if(now instanceof mxstarParser.StatementContext){
			System.out.printf("statement\n");
			if(now instanceof mxstarParser.S1Context){
				mxstarParser.S1Context ctx=(mxstarParser.S1Context)now;
				tmp.kind="braces";
				tmp.isscope=true;
				for(ParseTree i:ctx.statement())
					tmp.stmts.add(visit(i));
			}
			else if(now instanceof mxstarParser.S2Context){
				tmp.kind="empty";
			}
			else if(now instanceof mxstarParser.S3Context){
				mxstarParser.S3Context ctx=(mxstarParser.S3Context)now;
				tmp=visit(ctx.vardef());
			}
			else if(now instanceof mxstarParser.S4Context){
				mxstarParser.S4Context ctx=(mxstarParser.S4Context)now;
				tmp=visit(ctx.expr());
			}
			else if(now instanceof mxstarParser.S5Context){
				tmp.kind="break";
			}
			else if(now instanceof mxstarParser.S6Context){
				tmp.kind="continue";
			}
			else if(now instanceof mxstarParser.S7Context){
				mxstarParser.S7Context ctx=(mxstarParser.S7Context)now;
				tmp.kind="return";
				if(ctx.expr()!=null)
					tmp.expr.add(visit(ctx.expr()));
			}
			else if(now instanceof mxstarParser.S8Context){
				mxstarParser.S8Context ctx=(mxstarParser.S8Context)now;
				tmp.kind="if";
				tmp.expr.add(visit(ctx.expr()));
				for(ParseTree i:ctx.statement()){
					Node o;
					tmp.stmts.add(o=visit(i));
					if(o.kind=="vardef")
						o.isscope=true;
				}
			}
			else if(now instanceof mxstarParser.S9Context){
				mxstarParser.S9Context ctx=(mxstarParser.S9Context)now;
				tmp.kind="while";
				tmp.expr.add(visit(ctx.expr()));
				tmp.stmts.add(visit(ctx.statement()));
				if(tmp.stmts.get(0).kind=="vardef")
					tmp.stmts.get(0).isscope=true;
			}
			else if(now instanceof mxstarParser.S10Context){
				mxstarParser.S10Context ctx=(mxstarParser.S10Context)now;
				tmp.kind="for";
				for(int i=2,j=1;j<=3;i++,j++)
					if(ctx.getChild(i).getText().equals(";")||ctx.getChild(i).getText().equals(")"))
						tmp.expr.add(null);
					else tmp.expr.add(visit(ctx.getChild(i++)));
				tmp.stmts.add(visit(ctx.statement()));
				if(tmp.stmts.get(0).kind=="vardef")
					tmp.stmts.get(0).isscope=true;
			}
			else throw new Exception();
		}
		else if(now instanceof mxstarParser.ExprContext){
			System.out.printf("expr\n");
			if(now instanceof mxstarParser.E0Context||
				now instanceof mxstarParser.E1Context||
				now instanceof mxstarParser.E2Context||
				now instanceof mxstarParser.E3Context||
				now instanceof mxstarParser.E4Context||
				now instanceof mxstarParser.E5Context||
				now instanceof mxstarParser.E6Context){
					tmp.kind="const";
					tmp.id=now.getText();
			}
			else if(now instanceof mxstarParser.E7Context){
				tmp=visit(now.getChild(1));
			}
			else if(now instanceof mxstarParser.E8Context){
				mxstarParser.E8Context ctx=(mxstarParser.E8Context)now;
				tmp.kind="expr";
				tmp.id=".";
				tmp.expr.add(visit(ctx.expr()));
				tmp.names.add(ctx.Name().getText());
			}
			else if(now instanceof mxstarParser.E9Context){
				mxstarParser.E9Context ctx=(mxstarParser.E9Context)now;
				tmp.kind="expr";
				tmp.id="func";
				for(ParseTree i:ctx.expr())
					tmp.expr.add(visit(i));
			}
			else if(now instanceof mxstarParser.E10Context){
				mxstarParser.E10Context ctx=(mxstarParser.E10Context)now;
				tmp.kind="expr";
				tmp.id="[]";
				for(ParseTree i:ctx.expr())
					tmp.expr.add(visit(i));
			}
			else if(now instanceof mxstarParser.E11Context){
				mxstarParser.E11Context ctx=(mxstarParser.E11Context)now;
				tmp.kind="expr";
				String o=ctx.getChild(1).getText();
				if(o.equals("++"))tmp.id="++_";
				if(o.equals("--"))tmp.id="--_";
				tmp.expr.add(visit(ctx.expr()));
			}
			else if(now instanceof mxstarParser.E12Context){
				mxstarParser.E12Context ctx=(mxstarParser.E12Context)now;
				tmp.kind="expr";
				String o=ctx.getChild(0).getText();
				if(o.equals("++"))tmp.id="++";
				if(o.equals("--"))tmp.id="--";
				if(o.equals("~"))tmp.id="~";
				if(o.equals("!"))tmp.id="!";
				if(o.equals("-"))tmp.id="-";
				if(o.equals("+"))tmp.id="+";
				tmp.expr.add(visit(ctx.expr()));
			}
			else if(now instanceof mxstarParser.E13Context){
				mxstarParser.E13Context ctx=(mxstarParser.E13Context)now;
				tmp.kind="expr";
				tmp.id="new";
				tmp.names.add(ctx.getChild(1).getText());
				for(int i=3;i<ctx.getChildCount();i+=2){
					if(ctx.getChild(i).getText().equals("]"))tmp.expr.add(null);
					else if(ctx.getChild(i).getText().equals(")"));
					else{
						tmp.expr.add(visit(ctx.getChild(i)));
						i++;
					}
				}
			}
			else{
				tmp.kind="expr";
				String o=now.getChild(1).getText();
				if(o.equals("*"))tmp.id="*";
				if(o.equals("/"))tmp.id="/";
				if(o.equals("%"))tmp.id="%";
				if(o.equals("+"))tmp.id="+";
				if(o.equals("-"))tmp.id="-";
				if(o.equals("<<"))tmp.id="<<";
				if(o.equals(">>"))tmp.id=">>";
				if(o.equals("<"))tmp.id="<";
				if(o.equals(">"))tmp.id=">";
				if(o.equals("<="))tmp.id="<=";
				if(o.equals(">="))tmp.id=">=";
				if(o.equals("=="))tmp.id="==";
				if(o.equals("!="))tmp.id="!=";
				if(o.equals("&"))tmp.id="&";
				if(o.equals("^"))tmp.id="^";
				if(o.equals("|"))tmp.id="|";
				if(o.equals("&&"))tmp.id="&&";
				if(o.equals("||"))tmp.id="||";
				if(o.equals("="))tmp.id="=";
				tmp.expr.add(visit(now.getChild(0)));
				tmp.expr.add(visit(now.getChild(2)));
			}
		}
		else throw new Exception();
		t1--;
		return tmp;
	}
	static class Def{
		String kind;
		Type type;
		Map<String,Def>defs;
		List<Type>para;
		int id;
		Def(){
			defs=new TreeMap<String,Def>();
			para=new ArrayList<Type>();
		}
	}
	static class Code{
		String op;
		int c,a,b;
		Code(String o,int cc,int aa,int bb){
			op=o;c=cc;a=aa;b=bb;
		}
	}
	static List<Node>ancestor=new ArrayList<Node>();
	static int funct,vart=20,labt,globalvart,conststringt;
	static boolean addr;
	static Map<Integer,String>vkind=new TreeMap<Integer,String>();
	static Map vnum=new TreeMap();
	static List<String>conststr=new ArrayList<String>();
	static int lastregister;
	static Random rand=new Random();
	static void check(Node now)throws Exception{
		for(int i=1;i<=t2;i++)
			System.out.printf("\t");
		t2++;
		System.out.printf(now.kind+"\n");
		ancestor.add(now);
		if(now.kind=="code"){
			{
				Def d=new Def(),f;
				d.kind="class";
				now.scope.put("int",d);

				d=new Def();
				d.kind="class";
				now.scope.put("bool",d);

				d=new Def();
				d.kind="class";{
					f=new Def();
					f.kind="func";
					f.type=new Type("int");
					f.id=++funct;
					d.defs.put("length",f);

					f=new Def();
					f.kind="func";
					f.type=new Type("string");
					f.para.add(new Type("int"));
					f.para.add(new Type("int"));
					f.id=++funct;
					d.defs.put("substring",f);

					f=new Def();
					f.kind="func";
					f.type=new Type("int");
					f.id=++funct;
					d.defs.put("parseInt",f);

					f=new Def();
					f.kind="func";
					f.type=new Type("int");
					f.para.add(new Type("int"));
					f.id=++funct;
					d.defs.put("ord",f);
				}
				now.scope.put("string",d);

				f=new Def();
				f.kind="func";
				f.type=new Type("void");
				f.para.add(new Type("string"));
				f.id=++funct;
				now.scope.put("print",f);

				f=new Def();
				f.kind="func";
				f.type=new Type("void");
				f.para.add(new Type("string"));
				f.id=++funct;
				now.scope.put("println",f);

				f=new Def();
				f.kind="func";
				f.type=new Type("string");
				f.id=++funct;
				now.scope.put("getString",f);

				f=new Def();
				f.kind="func";
				f.type=new Type("int");
				f.id=++funct;
				now.scope.put("getInt",f);

				f=new Def();
				f.kind="func";
				f.type=new Type("string");
				f.para.add(new Type("int"));
				f.id=++funct;
				now.scope.put("toString",f);
				
				f=new Def();
				f.kind="func";
				f.type=new Type("int");
				f.id=++funct;
				now.scope.put("size!",f);
				
				f=new Def();
				f.kind="func";
				f.id=++funct;
				now.scope.put("string+",f);
				
				f=new Def();
				f.kind="func";
				f.id=++funct;
				now.scope.put("string<",f);
				
				f=new Def();
				f.kind="func";
				f.id=++funct;
				now.scope.put("string>",f);
				
				f=new Def();
				f.kind="func";
				f.id=++funct;
				now.scope.put("string<=",f);
				
				f=new Def();
				f.kind="func";
				f.id=++funct;
				now.scope.put("string>=",f);
				
				f=new Def();
				f.kind="func";
				f.id=++funct;
				now.scope.put("string==",f);
				
				f=new Def();
				f.kind="func";
				f.id=++funct;
				now.scope.put("string!=",f);
			}
			for(Node i:now.defs)
				if(i.kind=="classdef"){
					if(now.scope.get(i.id)!=null)
						throw new Exception();
					Def cdef=new Def();
					cdef.kind="class";
					int classvart=0;
					boolean hasconstruct=false;
					for(Node j:i.defs){
						if(j.kind=="funcdef"){
							if(cdef.defs.get(j.id)!=null)
								throw new Exception();
							Def fdef=new Def();
							fdef.kind="func";
							fdef.type=j.type;
							for(Type k:j.types)
								fdef.para.add(k);
							fdef.id=++funct;
							cdef.defs.put(j.id,fdef);
							i.scope.put(j.id,fdef);
							j.ir.add(new Code("funclab",fdef.id,0,0));
						}
						else if(j.kind=="vardef")
							for(String k:j.names){
								if(cdef.defs.get(k)!=null)
									throw new Exception();
								Def vdef=new Def();
								vdef.kind="var";
								vdef.type=j.type;
								vdef.id=++classvart;
								cdef.defs.put(k,vdef);
								i.scope.put(k,vdef);
							}
						else if(j.kind=="construct"){
							hasconstruct=true;
							if(!j.id.equals(i.id))
								throw new Exception();
							if(cdef.defs.get(j.id)!=null)
								throw new Exception();
							Def fdef=new Def();
							fdef.kind="func";
							fdef.id=++funct;
							cdef.defs.put(j.id,fdef);
							i.scope.put(j.id,fdef);
							j.ir.add(new Code("funclab",fdef.id,0,0));
							j.ir.add(1,new Code("get",j.ans=++vart,0,0));
						}
					}
					if(!hasconstruct){
						Node j=new Node();
						j.kind="construct";
						j.id=i.id;
						if(cdef.defs.get(j.id)!=null)
							throw new Exception();
						i.defs.add(j);
						Def fdef=new Def();
						fdef.kind="func";
						fdef.id=++funct;
						cdef.defs.put(j.id,fdef);
						i.scope.put(j.id,fdef);
						j.ir.add(new Code("funclab",fdef.id,0,0));
						j.ir.add(1,new Code("get",j.ans=++vart,0,0));
					}
					cdef.id=classvart;
					now.scope.put(i.id,cdef);
				}
				else if(i.kind=="funcdef"){
					if(now.scope.get(i.id)!=null)
						throw new Exception();
					Def fdef=new Def();
					fdef.kind="func";
					fdef.type=i.type;
					for(Type k:i.types)
						fdef.para.add(k);
					fdef.id=++funct;
					now.scope.put(i.id,fdef);
					i.ir.add(new Code("funclab",fdef.id,0,0));
				}
			boolean hasmain=false;
			for(Node i:now.defs)
				if(i.kind=="funcdef"&&i.id.equals("main")&&
					i.type.equal(new Type("int"))&&i.types.size()==0)
						hasmain=true;
			if(!hasmain)throw new Exception();
			Node j=new Node();
			j.kind="funcdef";
			now.defs.add(j);
			j.ir.add(new Code("funclab",0,0,0));
			for(Node i:now.defs)
				if(i!=j){
					check(i);
					now.ir.addAll(i.ir);
				}
			j.ir.add(new Code("call",now.scope.get("main").id,0,0));
			j.ir.add(new Code("ret",0,0,0));
			now.ir.addAll(j.ir);
		}
		else if(now.kind=="classdef"){
			Node j=null;
			for(Node i:now.defs)
				if(i.kind!="construct"){
					check(i);
					now.ir.addAll(i.ir);
				}
				else j=i;
			check(j);
			now.ir.addAll(j.ir);
		}
		else if(now.kind=="funcdef"){
			if(!now.type.equal(new Type("void"))&&(
				ancestor.get(0).scope.get(now.type.name)==null||
				ancestor.get(0).scope.get(now.type.name).kind!="class"))
					throw new Exception();
			if(ancestor.get(1).kind=="classdef")
				now.ir.add(new Code("get",now.ans=++vart,0,0));
			for(int i=0;i<now.types.size();i++){
				if(ancestor.get(0).scope.get(now.types.get(i).name)==null||
					ancestor.get(0).scope.get(now.types.get(i).name).kind!="class")
						throw new Exception();
				Def vdef=new Def();
				vdef.kind="var";
				vdef.type=now.types.get(i);
				vdef.id=++vart;
				now.scope.put(now.names.get(i),vdef);
				now.ir.add(new Code("get",vdef.id,0,0));
			}
			for(Node i:now.stmts){
				check(i);
				now.ir.addAll(i.ir);
			}
			now.ir.add(new Code("ret",0,0,0));
			now.ir.add(new Code("",0,0,0));
		}
		else if(now.kind=="construct"){
			for(Node i:now.stmts){
				check(i);
				now.ir.addAll(i.ir);
			}
			now.ir.add(new Code("ret",0,0,0));
			now.ir.add(new Code("",0,0,0));
		}
		else if(now.kind=="vardef"){
			if(ancestor.get(0).scope.get(now.type.name)==null||
				ancestor.get(0).scope.get(now.type.name).kind!="class")
					throw new Exception();
			Node fa=null;
			for(Node i:ancestor)
				if(i.isscope==true)
					fa=i;
			for(int i=0;i<now.names.size();i++){
				if(now.expr.get(i)!=null){
					check(now.expr.get(i));
					if(!now.type.equal(now.expr.get(i).type))
						throw new Exception();
				}
				if(fa.kind!="classdef"){
					if(fa.scope.get(now.names.get(i))!=null)
						throw new Exception();
					Def vdef=new Def();
					vdef.kind="var";
					vdef.type=now.type;
					vdef.id=++vart;
					if(fa.kind=="code"){
						vkind.put(vdef.id,"global");
						vnum.put(vdef.id,8*(++globalvart));
					}
					fa.scope.put(now.names.get(i),vdef);
				}
			}
			for(int i=0;i<now.names.size();i++)
				if(now.expr.get(i)!=null){
					if(fa.kind=="classdef"){
						Node z=null;
						for(Node ii:fa.defs)
							if(ii.type==null)
								z=ii;
						int v1=++vart,v2=++vart;
						vkind.put(v1,"const");
						vnum.put(v1,8*fa.scope.get(now.names.get(i)).id);
						z.ir.addAll(now.expr.get(i).ir);
						z.ir.add(new Code("+",v2,z.ans,v1));
						z.ir.add(new Code("save",v2,now.expr.get(i).ans,0));
					}
					else if(fa.kind=="code"){
						Node z=fa.defs.get(fa.defs.size()-1);
						z.ir.addAll(now.expr.get(i).ir);
						z.ir.add(new Code("mov",fa.scope.get(now.names.get(i)).id,now.expr.get(i).ans,0));
					}
					else{
						now.ir.addAll(now.expr.get(i).ir);
						now.ir.add(new Code("mov",fa.scope.get(now.names.get(i)).id,now.expr.get(i).ans,0));
					}
				}
		}
		else if(now.kind=="braces"){
			for(Node i:now.stmts){
				check(i);
				now.ir.addAll(i.ir);
			}
		}
		else if(now.kind=="empty");
		else if(now.kind=="break"||now.kind=="continue"){
			Node fa=null;
			for(Node i:ancestor)
				if(i.kind=="while"||i.kind=="for")
					fa=i;
			if(fa==null)throw new Exception();
			now.ir.add(new Code("jmp",now.kind=="break"?fa.ans+1:fa.ans+3,0,0));
		}
		else if(now.kind=="return"){
			Node fa=null;
			for(Node i:ancestor)
				if(i.kind=="funcdef"||i.kind=="construct")
					fa=i;
			if(fa==null)throw new Exception();
			if(fa.type==null||fa.type.equal(new Type("void"))){
				if(now.expr.size()!=0)
					throw new Exception();
				now.ir.add(new Code("ret",0,0,0));
			}
			else{
				if(now.expr.size()==0)
					throw new Exception();
				check(now.expr.get(0));
				if(!fa.type.equal(now.expr.get(0).type))
					throw new Exception();
				now.ir.addAll(now.expr.get(0).ir);
				now.ir.add(new Code("mov",1,now.expr.get(0).ans,0));
				now.ir.add(new Code("ret",0,0,0));
			}
		}
		else if(now.kind=="if"){
			int lab=labt;
			labt+=2;
			now.ans=lab;
			check(now.expr.get(0));
			if(!now.expr.get(0).type.equal(new Type("bool")))
				throw new Exception();
			for(Node i:now.stmts)
				check(i);
			now.ir.addAll(now.expr.get(0).ir);
			now.ir.add(new Code("test",now.expr.get(0).ans,0,0));
			now.ir.add(new Code("jnz",lab+1,0,0));
			if(now.stmts.size()==2)
				now.ir.addAll(now.stmts.get(1).ir);
			now.ir.add(new Code("jmp",lab+2,0,0));
			now.ir.add(new Code("label",lab+1,0,0));
			now.ir.addAll(now.stmts.get(0).ir);
			now.ir.add(new Code("label",lab+2,0,0));
		}
		else if(now.kind=="while"){
			int lab=labt;
			labt+=3;
			now.ans=lab;
			check(now.expr.get(0));
			if(!now.expr.get(0).type.equal(new Type("bool")))
				throw new Exception();
			check(now.stmts.get(0));
			now.ir.add(new Code("label",lab+2,0,0));
			now.ir.addAll(now.expr.get(0).ir);
			now.ir.add(new Code("test",now.expr.get(0).ans,0,0));
			now.ir.add(new Code("jz",lab+1,0,0));
			now.ir.addAll(now.stmts.get(0).ir);
			now.ir.add(new Code("label",lab+3,0,0));
			now.ir.add(new Code("jmp",lab+2,0,0));
			now.ir.add(new Code("label",lab+1,0,0));
		}
		else if(now.kind=="for"){
			int lab=labt;
			labt+=3;
			now.ans=lab;
			for(Node i:now.expr)
				if(i!=null)check(i);
			if(now.expr.get(1)!=null&&!now.expr.get(1).type.equal(new Type("bool")))
				throw new Exception();
			check(now.stmts.get(0));
			if(now.expr.get(0)!=null)
				now.ir.addAll(now.expr.get(0).ir);
			now.ir.add(new Code("label",lab+2,0,0));
			if(now.expr.get(1)!=null){
				now.ir.addAll(now.expr.get(1).ir);
				now.ir.add(new Code("test",now.expr.get(1).ans,0,0));
				now.ir.add(new Code("jz",lab+1,0,0));
			}
			now.ir.addAll(now.stmts.get(0).ir);
			now.ir.add(new Code("label",lab+3,0,0));
			if(now.expr.get(2)!=null)
				now.ir.addAll(now.expr.get(2).ir);
			now.ir.add(new Code("jmp",lab+2,0,0));
			now.ir.add(new Code("label",lab+1,0,0));
		}
		else if(now.kind=="const"){
			if(now.id.equals("this")){
				if(ancestor.get(1).kind=="classdef")
					now.type=new Type(ancestor.get(1).id);
				else throw new Exception();
				now.ans=ancestor.get(2).ans;
			}
			else if(now.id.charAt(0)>='0'&&now.id.charAt(0)<='9'){
				now.type=new Type("int");
				now.ans=++vart;
				vkind.put(now.ans,"const");
				vnum.put(now.ans,Integer.valueOf(now.id));
				now.ir.add(new Code("mov",++vart,now.ans,0));
				now.ans=vart;
			}
			else if(now.id.equals("true")){
				now.type=new Type("bool");
				now.ans=++vart;
				vkind.put(now.ans,"const");
				vnum.put(now.ans,1);
				now.ir.add(new Code("mov",++vart,now.ans,0));
				now.ans=vart;
			}
			else if(now.id.equals("false")){
				now.type=new Type("bool");
				now.ans=++vart;
				vkind.put(now.ans,"const");
				vnum.put(now.ans,0);
				now.ir.add(new Code("mov",++vart,now.ans,0));
				now.ans=vart;
			}
			else if(now.id.charAt(0)=='"'){
				now.type=new Type("string");
				now.ans=++vart;
				conststr.add(now.id);
				vkind.put(now.ans,"string");
				vnum.put(now.ans,++conststringt);
			}
			else if(now.id.equals("null")){
				now.type=new Type("null");
				now.ans=++vart;
				vkind.put(now.ans,"const");
				vnum.put(now.ans,0);
				now.ir.add(new Code("mov",++vart,now.ans,0));
				now.ans=vart;
			}
			else{
				Node fr=null;
				Def d=null;
				for(Node i:ancestor)
					if(i.scope.get(now.id)!=null){
						fr=i;
						d=i.scope.get(now.id);
					}
				if(d==null||d.kind=="class")
					throw new Exception();
				if(d.kind=="var"){
					now.type=d.type;
					now.leftv=true;
				}
				else{
					now.type=new Type("*");
					now.types.add(d.type);
					now.types.addAll(d.para);
				}
				if(d.kind=="var"&&fr.kind=="classdef"){
					Node f=null;
					for(Node i:ancestor)
						if(i.kind=="funcdef"||i.kind=="construct")
							f=i;
					int v1=++vart,v2=++vart;
					vkind.put(v1,"const");
					vnum.put(v1,8*d.id);
					now.ir.add(new Code("+",v2,f.ans,v1));
					now.ans=v2;
					if(!addr){
						now.ir.add(new Code("load",++vart,now.ans,0));
						now.ans=vart;
					}
					else now.ismem=true;
				}
				else now.ans=d.id;
			}
			System.out.printf("type/const:"+now.type.name+"\n");
		}
		else if(now.kind=="expr"){
			if(now.id=="."){
				boolean paddr=addr;
				addr=false;
				check(now.expr.get(0));
				addr=paddr;
				Type type=now.expr.get(0).type;
				if(type.array!=0){
					if(!now.names.get(0).equals("size"))
						throw new Exception();
					now.type=new Type("*");
					now.types.add(new Type("int"));
					Def f=ancestor.get(0).scope.get("size!");
					now.ans=f.id;
				}
				else{
					Def d=ancestor.get(0).scope.get(type.name);
					if(d==null||d.defs.get(now.names.get(0))==null)
						throw new Exception();
					d=d.defs.get(now.names.get(0));
					if(d.kind=="func"){
						now.type=new Type("*");
						now.types.add(d.type);
						now.types.addAll(d.para);
						now.ans=d.id;
					}
					else{
						now.type=d.type;
						now.leftv=true;
						now.ir.addAll(now.expr.get(0).ir);
						int v1=++vart,v2=++vart;
						vkind.put(v1,"const");
						vnum.put(v1,8*d.id);
						now.ir.add(new Code("+",v2,now.expr.get(0).ans,v1));
						now.ans=v2;
						if(!addr){
							now.ir.add(new Code("load",++vart,now.ans,0));
							now.ans=vart;
						}
						else now.ismem=true;
					}
				}
			}
			else if(now.id=="func"){
				for(Node i:now.expr)
					check(i);
				Node f=now.expr.get(0);
				if(f.type.name!="*"||f.types.size()!=now.expr.size())
					throw new Exception();
				for(int i=1;i<f.types.size();i++)
					if(!f.types.get(i).equal(now.expr.get(i).type))
						throw new Exception();
				now.type=f.types.get(0);
				if(f.kind=="expr")
					now.ir.addAll(f.expr.get(0).ir);
				for(int i=1;i<now.expr.size();i++)
					now.ir.addAll(now.expr.get(i).ir);
				if(f.kind=="expr")
					now.ir.add(new Code("send",f.expr.get(0).ans,0,0));
				else if(ancestor.get(1).kind=="classdef"&&ancestor.get(1).scope.get(f.id)!=null)
					now.ir.add(new Code("send",ancestor.get(2).ans,0,0));
				for(int i=1;i<now.expr.size();i++)
					now.ir.add(new Code("send",now.expr.get(i).ans,0,0));
				now.ir.add(new Code("call",f.ans,0,0));
				now.ir.add(new Code("mov",++vart,1,0));
				now.ans=vart;
			}
			else if(now.id=="[]"){
				boolean paddr=addr;
				if(addr==true)addr=false;
				for(Node i:now.expr)
					check(i);
				addr=paddr;
				if(now.expr.get(0).type.array==0||
					!now.expr.get(1).type.equal(new Type("int")))
						throw new Exception();
				now.type=new Type(now.expr.get(0).type.name);
				now.type.array=now.expr.get(0).type.array-1;
				now.leftv=true;
				now.ir.addAll(now.expr.get(0).ir);
				now.ir.addAll(now.expr.get(1).ir);
				now.ans=++vart;
				now.ir.add(new Code("lea",now.ans,now.expr.get(0).ans,now.expr.get(1).ans));
				if(!addr){
					now.ir.add(new Code("load",++vart,now.ans,0));
					now.ans=vart;
				}
				else now.ismem=true;
			}
			else if(now.id=="new"){
				Def d=ancestor.get(0).scope.get(now.names.get(0));
				if(d==null||d.kind!="class")throw new Exception();
				boolean empty=false;
				int num=0;
				for(Node i:now.expr){
					if(i!=null){
						num++;
						if(empty)throw new Exception();
						check(i);
						if(!i.type.equal(new Type("int")))
							throw new Exception();
						now.ir.addAll(i.ir);
					}
					else empty=true;
				}
				now.type=new Type(now.names.get(0));
				now.type.array=now.expr.size();
				if(num!=0){
					for(int i=1;i<=num;i++)
						now.ir.add(new Code("mov",vart+i,now.expr.get(i-1).ans,0));
					vart+=num;
					int v0=vart-num,ir0=now.ir.size(),vv0=vart+1;
					vart+=num;
					for(int i=num;i!=0;i--){
						int v1=++vart,v2=++vart;
						vkind.put(v2,"const");
						vnum.put(v2,0);
						now.ir.add(ir0,new Code("malloc",vv0+i,v0+i,0));
						if(i!=num){
							labt+=2;
							now.ir.add(ir0+1,new Code("mov",v1,v2,0));
							now.ir.add(ir0+2,new Code("label",labt-1,0,0));
							now.ir.add(ir0+3,new Code("<",++vart,v1,v0+i));
							now.ir.add(ir0+4,new Code("jz",labt,0,0));
							now.ir.add(new Code("lea",++vart,vv0+i,v1));
							now.ir.add(new Code("save",vart,vv0+i+1,0));
							now.ir.add(new Code("++",v1,0,0));
							now.ir.add(new Code("jmp",labt-1,0,0));
							now.ir.add(new Code("label",labt,0,0));
						}
					}
					now.ans=vv0+1;
				}
				else{
					int v1=++vart,v2=++vart;
					vkind.put(v2,"const");
					vnum.put(v2,8*d.id);
					now.ir.add(new Code("malloc",v1,v2,0));
					now.ir.add(new Code("send",v1,0,0));
					now.ir.add(new Code("call",d.defs.get(now.names.get(0)).id,0,0));
					now.ans=v1;
				}
			}
			else if(now.expr.size()==1){
				boolean paddr=addr;
				if(now.id=="++_"||now.id=="--_"||now.id=="++"||now.id=="--")
					addr=true;
				check(now.expr.get(0));
				addr=paddr;
				Type type=now.expr.get(0).type;
				if(now.id=="++_"||now.id=="--_"||
					now.id=="++"||now.id=="--"||
					now.id=="~"||now.id=="-"||now.id=="+"){
						if(!type.equal(new Type("int")))
							throw new Exception();
				}
				else if(now.id=="!"){
					if(!type.equal(new Type("bool")))
						throw new Exception();
				}
				else throw new Exception();
				if(now.id=="++_"||now.id=="--_"||now.id=="++"||now.id=="--")
					if(!now.expr.get(0).leftv)throw new Exception();
				if(now.id=="++"||now.id=="--")
					now.leftv=true;
				now.type=type;
				now.ir.addAll(now.expr.get(0).ir);
				if(now.id=="++_"||now.id=="--_"){
					if(now.expr.get(0).ismem){
						int v1=++vart,v2=++vart;
						now.ir.add(new Code("load",v1,now.expr.get(0).ans,0));
						now.ir.add(new Code("mov",v2,v1,0));
						now.ir.add(new Code(now.id=="++_"?"++":"--",v1,0,0));
						now.ir.add(new Code("save",now.expr.get(0).ans,v1,0));
						now.ans=v2;
					}
					else{
						int v1=++vart;
						now.ir.add(new Code("mov",v1,now.expr.get(0).ans,0));
						now.ir.add(new Code(now.id=="++_"?"++":"--",now.expr.get(0).ans,0,0));
						now.ans=v1;
					}
				}
				else if(now.id=="++"||now.id=="--"){
					if(now.expr.get(0).ismem){
						int v1=++vart;
						now.ir.add(new Code("load",v1,now.expr.get(0).ans,0));
						now.ir.add(new Code(now.id,v1,0,0));
						now.ir.add(new Code("save",now.expr.get(0).ans,v1,0));
						now.ans=now.expr.get(0).ans;
						now.ismem=true;
					}
					else{
						now.ir.add(new Code(now.id,now.expr.get(0).ans,0,0));
						now.ans=now.expr.get(0).ans;
					}
				}
				else{
					if(now.id!="+")
						if(now.id!="-")now.ir.add(new Code(now.id,now.expr.get(0).ans,0,0));
						else now.ir.add(new Code("neg",now.expr.get(0).ans,0,0));
					now.ans=now.expr.get(0).ans;
				}
				if(now.ismem&&!addr){
					now.ir.add(new Code("load",++vart,now.ans,0));
					now.ans=vart;
				}
			}
			else{
				boolean paddr=addr;
				if(now.id=="=")
					addr=true;
				check(now.expr.get(0));
				addr=paddr;
				check(now.expr.get(1));
				Type t1=now.expr.get(0).type,t2=now.expr.get(1).type;
				String o=now.id;
				if(o=="*"||o=="/"||o=="%"||o=="<<"||o==">>"||o=="&"||o=="|"||o=="^"||o=="-"){
					if(!(t1.equal(t2)&&t1.equal(new Type("int"))))
						throw new Exception();
					now.type=t1;
				}
				else if(o=="+"||o==">"||o=="<"||o==">="||o=="<="){
					if(!(t1.equal(t2)&&(t1.equal(new Type("int"))||t1.equal(new Type("string")))))
						throw new Exception();
					now.type=o=="+"?t1:new Type("bool");
				}
				else if(o=="&&"||o=="||"){
					if(!(t1.equal(t2)&&t1.equal(new Type("bool"))))
						throw new Exception();
					now.type=t1;
				}
				else if(o=="=="||o=="!="){
					if(!t1.equal(t2))
						throw new Exception();
					now.type=new Type("bool");
				}
				else if(o=="="){
					if(!t1.equal(t2)||now.expr.get(0).leftv==false)
						throw new Exception();
					now.type=t1;
				}
				else throw new Exception();
				if(o=="&&"||o=="||"){
					int lab=labt,v=++vart;
					labt+=2;
					now.ir.addAll(now.expr.get(0).ir);
					now.ir.add(new Code("test",now.expr.get(0).ans,0,0));
					now.ir.add(new Code("jnz",lab+1,0,0));
					if(o=="&&"){
						int vv=++vart;
						vkind.put(vv,"const");
						vnum.put(vv,0);
						now.ir.add(new Code("mov",v,vv,0));
					}
					else{
						now.ir.addAll(now.expr.get(1).ir);
						now.ir.add(new Code("mov",v,now.expr.get(1).ans,0));
					}
					now.ir.add(new Code("jmp",lab+2,0,0));
					now.ir.add(new Code("label",lab+1,0,0));
					if(o=="||"){
						int vv=++vart;
						vkind.put(vv,"const");
						vnum.put(vv,1);
						now.ir.add(new Code("mov",v,vv,0));
					}
					else{
						now.ir.addAll(now.expr.get(1).ir);
						now.ir.add(new Code("mov",v,now.expr.get(1).ans,0));
					}
					now.ir.add(new Code("label",lab+2,0,0));
					now.ans=v;
				}
				else if(o=="="){
					now.ir.addAll(now.expr.get(0).ir);
					now.ir.addAll(now.expr.get(1).ir);
					if(now.expr.get(0).ismem)
						now.ir.add(new Code("save",now.expr.get(0).ans,now.expr.get(1).ans,0));
					else now.ir.add(new Code("mov",now.expr.get(0).ans,now.expr.get(1).ans,0));
					now.ans=now.expr.get(0).ans;
					now.ismem=now.expr.get(0).ismem;
					if(now.ismem&&!addr){
						now.ir.add(new Code("load",++vart,now.ans,0));
						now.ans=vart;
					}
				}
				else if((o=="+"||o==">"||o=="<"||o==">="||o=="<="||o=="=="||o=="!=")&&now.expr.get(0).type.equal(new Type("string"))){
					now.ir.addAll(now.expr.get(0).ir);
					now.ir.addAll(now.expr.get(1).ir);
					now.ir.add(new Code("send",now.expr.get(0).ans,0,0));
					now.ir.add(new Code("send",now.expr.get(1).ans,0,0));
					now.ir.add(new Code("call",ancestor.get(0).scope.get("string"+o).id,0,0));
					now.ir.add(new Code("mov",++vart,1,0));
					now.ans=vart;
				}
				else{
					now.ir.addAll(now.expr.get(0).ir);
					now.ir.addAll(now.expr.get(1).ir);
					int v=++vart;
					now.ir.add(new Code(now.id,v,now.expr.get(0).ans,now.expr.get(1).ans));
					now.ans=v;
				}
			}
			System.out.printf("type/expr:"+now.type.name+"\n");
		}
		ancestor.remove(ancestor.size()-1);
		t2--;
	}
	static String regname[]={"","rax","rbx","rcx","rdx","rbp","rsp","rdi","rsi","r8","r9","r10","r11","r12","r13","r14","r15"};
	static String ans="";
	static String varstring(int v)throws Exception{
		String k=vkind.get(v);
		int vv=(int)vnum.get(v);
		if(k=="const")return""+vv;
		else if(k=="global")return"qword[gbl+"+vv+"]";
		else if(k=="string")return"S"+vv;
		else if(k=="stack")return"qword[rsp+"+vv+"]";
		else if(k=="register")return regname[vv];
		else throw new Exception();
	}
	static int toregister(int now)throws Exception{
		if(vkind.get(now)=="global"||vkind.get(now)=="stack"){
			ans+="\tmov "+regname[(lastregister^=rand.nextInt(3)+1)+1]+","+varstring(now)+"\n";
			now=lastregister+1;
		}
		return now;
	}
	static void codegen(List<Code>ir)throws Exception{
		BufferedReader reader=new BufferedReader(new FileReader("./builtin1.txt"));
		for(String line;(line=reader.readLine())!=null;)
			ans+=line+"\n";
		for(int i=0;i<ir.size();i++){
			Code o=ir.get(i);
			if(o.op.equals("")){
				System.out.printf("\n");
			}
			else{
				System.out.print(o.op);
				System.out.printf(" %d %d %d\n",o.c,o.a,o.b);
			}
			if(o.op==""){
				ans+="\n";
			}
			else if(o.op=="funclab"){
				if(o.c==0)ans+="main:\n";
				else ans+="F"+o.c+":\n";
				ans+="\tpush rbp\n";
				ans+="\tmov rbp,rsp\n";
				ans+="\tsub rsp,"+o.a+"\n";
			}
			else if(o.op=="label"){
				ans+="L"+o.c+":\n";
			}
			else if(o.op=="send"){
				int j=i;
				while(ir.get(j).op=="send")j++;
				for(int w=i;w<j;w++){
					int cc=toregister(ir.get(w).c);
					ans+="\tmov "+(w<=i+1?regname[w-i+7]:"qword[arg+"+8*(w-i-1)+"]")+","+varstring(cc)+"\n";
				}
				i=j-1;
			}
			else if(o.op=="get"){
				int j=i;
				while(ir.get(j).op=="get")j++;
				for(int w=i;w<j;w++){
					int cc=vkind.get(ir.get(w).c)=="global"||vkind.get(ir.get(w).c)=="stack"?(lastregister^=rand.nextInt(3)+1)+1:ir.get(w).c;
					ans+="\tmov "+varstring(cc)+","+(w<=i+1?regname[w-i+7]:"qword[arg+"+8*(w-i-1)+"]")+"\n";
					if(cc<=4)ans+="\tmov "+varstring(ir.get(w).c)+","+regname[cc]+"\n";
				}
				i=j-1;
			}
			else if(o.op=="call"){
				if(o.c==1){
					ans+="\txor rax,rax\n";
					ans+="\tmov al,byte[rdi]\n";
				}
				else if(o.c==10){
					ans+="\tmov rax,qword[rdi]\n";
				}
				else if(o.c==5||o.c==6){
					ans+="\tmov rsi,rdi\n";
					ans+="\tinc rsi\n";
					ans+="\tmov rdi,"+(o.c==5?"format\n":"formatln\n");
					ans+="\txor rax,rax\n";
					ans+="\tcall printf\n";
				}
				else if(o.c<=17){
					String ss[]={"","length","substring","parseInt","ord","print","println","getString","getInt","toString","size!","concat","strls","strgt","strle","strge","streq","strne"};
					ans+="\tcall "+ss[o.c]+"\n";
				}
				else ans+="\tcall F"+o.c+"\n";
			}
			else if(o.op=="ret"){
				ans+="\tleave\n";
				ans+="\tret\n";
			}
			else if(o.op=="save"){
				int cc=toregister(o.c),aa=toregister(o.a);
				ans+="\tmov qword["+varstring(cc)+"],"+varstring(aa)+"\n";
			}
			else if(o.op=="load"){
				int cc=vkind.get(o.c)=="global"||vkind.get(o.c)=="stack"?(lastregister^=rand.nextInt(3)+1)+1:o.c;
				int aa=toregister(o.a);
				ans+="\tmov "+varstring(cc)+",qword["+varstring(aa)+"]\n";
				if(cc<=4)ans+="\tmov "+varstring(o.c)+","+regname[cc]+"\n";
			}
			else if(o.op=="mov"){
				int cc=o.c,aa=o.a;
				if((vkind.get(cc)=="global"||vkind.get(cc)=="stack"))
					aa=toregister(aa);
				ans+="\tmov "+varstring(cc)+","+varstring(aa)+"\n";
			}
			else if(o.op=="lea"){
				int cc=vkind.get(o.c)=="global"||vkind.get(o.c)=="stack"?(lastregister^=rand.nextInt(3)+1)+1:o.c;
				int aa=toregister(o.a),bb=toregister(o.b);
				ans+="\tlea "+varstring(cc)+",["+varstring(aa)+"+8*"+varstring(bb)+"+8]\n";
				if(cc<=4)ans+="\tmov "+varstring(o.c)+","+regname[cc]+"\n";
			}
			else if(o.op=="malloc"){
				ans+="\tmov rdi,"+varstring(o.a)+"\n";
				ans+="\tshl rdi,3\n";
				ans+="\tadd rdi,8\n";
				ans+="\tcall malloc\n";
				ans+="\tmov "+varstring(o.c)+",rax\n";
				ans+="\tmov "+regname[(lastregister=rand.nextInt(3)+1)+1]+","+varstring(o.a)+"\n";
				ans+="\tmov qword[rax],"+regname[lastregister+1]+"\n";
			}
			else if(o.op=="jmp"){
				ans+="\tjmp L"+o.c+"\n";
			}
			else if(o.op=="jz"||o.op=="jnz"){
				ans+="\t"+o.op+" L"+o.c+"\n";
			}
			else if(o.op=="test"){
				int cc=toregister(o.c);
				ans+="\ttest "+varstring(cc)+","+varstring(cc)+"\n";
			}
			else{
				if(o.op=="+"||o.op=="-"||o.op=="&"||o.op=="|"||o.op=="^"){
					int cc=vkind.get(o.c)=="global"||vkind.get(o.c)=="stack"?(lastregister^=rand.nextInt(3)+1)+1:o.c;
					ans+="\tmov "+varstring(cc)+","+varstring(o.a)+"\n";
					if(o.op=="+")ans+="\tadd ";
					else if(o.op=="-")ans+="\tsub ";
					else if(o.op=="&")ans+="\tand ";
					else if(o.op=="|")ans+="\tor ";
					else if(o.op=="^")ans+="\txor ";
					else throw new Exception();
					ans+=varstring(cc)+","+varstring(o.b)+"\n";
					if(cc<=4)ans+="\tmov "+varstring(o.c)+","+regname[cc]+"\n";
				}
				else if(o.op=="<"||o.op==">"||o.op=="<="||o.op==">="||o.op=="=="||o.op=="!="){
					int aa=o.a,bb=o.b;
					if(vkind.get(aa)=="global"||vkind.get(aa)=="stack")
						bb=toregister(bb);
					int zz=(lastregister^=rand.nextInt(3)+1)+1;
					ans+="\txor "+regname[zz]+","+regname[zz]+"\n";
					ans+="\tcmp "+varstring(aa)+","+varstring(bb)+"\n";
					if(o.op=="<")ans+="\tsetl ";
					else if(o.op=="<=")ans+="\tsetle ";
					else if(o.op==">")ans+="\tsetg ";
					else if(o.op==">=")ans+="\tsetge ";
					else if(o.op=="==")ans+="\tsete ";
					else if(o.op=="!=")ans+="\tsetne ";
					else throw new Exception();
					ans+=regname[zz].charAt(1)+"l\n";
					ans+="\tmov "+varstring(o.c)+","+regname[zz]+"\n";
				}
				else if(o.op=="*"){
					ans+="\tmov rax,"+varstring(o.a)+"\n";
					ans+="\timul "+varstring(o.b)+"\n";
					ans+="\tmov "+varstring(o.c)+",rax\n";
				}
				else if(o.op=="/"||o.op=="%"){
					ans+="\tmov rdx,0\n";
					ans+="\tmov rax,"+varstring(o.a)+"\n";
					ans+="\tdiv "+varstring(o.b)+"\n";
					ans+="\tmov "+varstring(o.c)+","+(o.op=="/"?"rax\n":"rdx\n");
				}
				else if(o.op=="<<"||o.op==">>"){
					int cc=vkind.get(o.c)=="global"||vkind.get(o.c)=="stack"?(lastregister=2^(rand.nextInt(3)+1))+1:o.c;
					ans+="\tmov "+varstring(cc)+","+varstring(o.a)+"\n";
					ans+="\tmov rcx,"+varstring(o.b)+"\n";
					ans+=(o.op=="<<"?"\tshl ":"\tshr ")+varstring(cc)+",cl\n";
					if(cc<=4)ans+="\tmov "+varstring(o.c)+","+regname[cc]+"\n";
				}
				else if(o.op=="++"||o.op=="--"||o.op=="neg"||o.op=="!"||o.op=="~"){
					int cc=toregister(o.c);
					if(o.op=="++")ans+="\tinc ";
					else if(o.op=="--")ans+="\tdec ";
					else if(o.op=="!")ans+="\txor "+varstring(cc)+",1\n";
					else if(o.op=="~")ans+="\tnot ";
					else if(o.op=="neg")ans+="\tneg ";
					else throw new Exception();
					if(o.op!="!")ans+=varstring(cc)+"\n";
					if(cc<=4)ans+="\tmov "+varstring(o.c)+","+regname[cc]+"\n";
				}
				else throw new Exception();
			}
		}
		reader=new BufferedReader(new FileReader("./builtin2.txt"));
		for(String line;(line=reader.readLine())!=null;)
			ans+=line+"\n";
		for(int i=0;i<conststr.size();i++){
			ans+="S"+(i+1)+":\n";
			ans+="\tdb ";
			List<Integer>z=new ArrayList<Integer>();
			boolean zy=false;
			for(int j=1;j+1<conststr.get(i).length();j++){
				char w=conststr.get(i).charAt(j);
				if(zy==true){
					if(w=='n')z.add((int)'\n');
					else if(w=='\\')z.add((int)'\\');
					else if(w=='r')z.add((int)'\r');
					else if(w=='t')z.add((int)'\t');
					else if(w=='"')z.add((int)'"');
					else throw new Exception();
					zy=false;
				}
				else if(w=='\\')zy=true;
				else z.add((int)w);
			}
			ans+=z.size();
			for(int j=0;j<z.size();j++)
				ans+=","+z.get(j);
			ans+=",0\n";
		}
	}
	static void regalloc(List<Code>ir){
		for(int i=1;i<=16;i++){
			vkind.put(i,"register");
			vnum.put(i,i);
		}
		for(int i=0;i<ir.size();i++){
			int j=i+1;
			while(j<ir.size()&&ir.get(j).op!="funclab")j++;
			int z=0;
			for(int k=i;k<j;k++){
				String op=ir.get(k).op;
				if(op=="funclab"||op=="label"||op=="jz"||op=="jnz"||op=="jmp"||op=="call")
					continue;
				int c=ir.get(k).c,a=ir.get(k).a,b=ir.get(k).b;
				if(c!=0&&vkind.get(c)==null){
					vkind.put(c,"stack");
					vnum.put(c,8*(++z));
				}
				if(a!=0&&vkind.get(a)==null){
					vkind.put(a,"stack");
					vnum.put(a,8*(++z));
				}
				if(b!=0&&vkind.get(b)==null){
					vkind.put(b,"stack");
					vnum.put(b,8*(++z));
				}
			}
			ir.get(i).a=8*z+8;
			i=j-1;
		}
	}
	public static void main(String[] args)throws IOException,Exception{
		System.setOut(new PrintStream(new FileOutputStream(new File("./test/debug.txt"))));
		Node root=visit((new mxstarParser(new CommonTokenStream(new mxstarLexer(new ANTLRInputStream(new FileInputStream("./test/test.txt")))))).code());
		check(root);
		regalloc(root.ir);
		codegen(root.ir);
		System.setOut(new PrintStream(new FileOutputStream(new File("./test/test.asm"))));
		System.out.print(ans);
	}
}
