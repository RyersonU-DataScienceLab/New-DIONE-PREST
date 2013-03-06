

package treeparser;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import treeparser.treeobject.BaseParsedObject;
import treeparser.treeobject.FunctionCall;
import treeparser.treeobject.ParsedObject;
import treeparser.treeobject.ParsedObjectLeaf;
import treeparser.treeobject.Variable;

/**
 *
 * @author Tomi
 */
public class FunctionAnalyzer {
    
    public static List<Variable> findVariables(ParsedObject funcParams, ParsedObject functionBody){
        int index;
        List<Variable> declarations=new ArrayList<Variable>();
        Variable var=null;
        int size=functionBody.getNumberOfChildren();
        /*
         * We search for variable declarations that start with word that represents 
         * simple type, eg int,(see Constants class for full listing) or word followed by ::
         * 
         */
        if(functionBody.getType()==Type.BRACKET){
            for(index=0;index<size;index++){
                if(index==0 || index==size-1){}
                else{
                    BaseParsedObject obj=functionBody.getChildren().get(index);
                    BaseParsedObject nextObj=functionBody.getChildren().get(index+1);
                    if(obj instanceof ParsedObjectLeaf){
                        if(obj.getType()==Type.WORDTOKEN){
                            System.out.println("Word "+obj.toString());
                            if(nextObj.getContent().contentEquals("::")){
                                index+=parseVariable(index, functionBody, declarations);
                            }else if(Constants.isSimpleType(obj.getContent())){
                                index+=parsePrimitiveVariable(index, functionBody, declarations);
                            }
                        }


                    }else if(obj instanceof ParsedObject){}
                }
            }
        }
        
        return null;
    }
    public static List<FunctionCall> findFunctionCalls(ParsedObject object){
        List<FunctionCall> fcs=new ArrayList();
        return findFunctionCalls(object,fcs);
    }
    /*
     * Found function calls are collected to the functions list.
     * This method is called recursively so it's private. 
     */
    private static List<FunctionCall> findFunctionCalls(ParsedObject object, List<FunctionCall> functions){
         List<FunctionCall> fcs=functions;
        if(object.getType()==Type.BRACKET){
            //ParsedObject sentence=new ParsedObject(null,"Sentence",Type.SENTENCE);
            List<BaseParsedObject> list=object.getChildren();
            Iterator<BaseParsedObject> it=list.iterator();
            String bracket;
            boolean firstTime=true;
            
            String lastWord=null;
            
            while(it.hasNext()){
                
                BaseParsedObject obj=it.next();
                
                if(firstTime){
                    /*if(obj instanceof ParsedObjectLeaf){
                        bracket=object.getContent();
                    }else
                        System.out.println("Expected bracket leaf not found:"+ obj.getContent());*/
                    firstTime=false;
                }else if(!canIgnore(obj)){

                    if(obj instanceof ParsedObjectLeaf){
                        ParsedObjectLeaf pol=(ParsedObjectLeaf) obj;
                        if(pol.getType()==Type.WORDTOKEN){
                            lastWord=pol.getContent();
                            if(Constants.isKeyword(lastWord))
                                lastWord=null;
                        }else
                            lastWord=null;
                    }else if(obj instanceof ParsedObject){
                        
                        ParsedObject po=(ParsedObject)obj;
                        char b=getBracketType(po);
                        if(b=='('){
                            if(lastWord!=null){
                                FunctionCall f=createFunctionCall(po.getParent(),po);
                                if(f!=null)
                                    fcs.add(f);
                                else
                                    System.out.println(lastWord+" called");
                            }
                            findFunctionCalls(po, fcs);
                        }else if(b=='{'){
                            findFunctionCalls(po, fcs);
                        }
                        lastWord=null;
                    }
                }
            }
            
        }
        return fcs;
        
    }
    
    private static boolean canIgnore(BaseParsedObject obj) {
        Type t=obj.getType();
        if(t==Type.COMMENT)
            return true;
        else if(t==Type.NEWLINE)
            return true;
        return false;
    }
    
    private static char getBracketType(ParsedObject obj){
            
            if(obj.getChildren().get(0) instanceof ParsedObjectLeaf)
                return ((ParsedObjectLeaf)obj.getChildren().get(0)).getContent().charAt(0);
            else throw new Error("Not a bracket object");
        
    }
/**
 * This method picks all attributes from ParsedObject to form one function call
 * @param obj ParsedObject that contains all components that are needed to create FunctionCall
 * (function name, parameters, owner of the function)
 * @param parameters parameter ParsedObject is used to find where is the last piece of the function call
 * 
 * example of ParsedObject obj:
 * {
 *  int a;
 *  abc::cd::hello(a); //contains ParsedObjectLeaf "abc", "::", "hello" and ParsedObject parameters="(a)".
 * }
 * createFunctionCall(obj,parameters) would return FunctionCall with ParsedObject owner={"abc","::", "cd"}, name="hello" and parameters=(a)
 */
    private static FunctionCall createFunctionCall(ParsedObject obj, ParsedObject parameters) {
        int i=obj.getChildren().indexOf(parameters);
        ParsedObject po=new ParsedObject(null,"FunctionCall",Type.OTHER);
        ParsedObjectLeaf name=null;
        BaseParsedObject temp;
        int lastRef=0, end=-1;
        if(i==-1)
            throw new UnsupportedOperationException("Function parameters not found in obj");
        else{
            if((i-1>=0)){
                if(obj.getChildren().get(i-1) instanceof ParsedObjectLeaf){
                    name=(ParsedObjectLeaf)obj.getChildren().get(i-1);
                    
                }else{ 
                    System.out.println("null returned");
                    return null;
                }
            }
            //
            for(int x=1;(i-x)>0;x++){//This loop looks back from name of the function to find out how many owners it has
                temp=obj.getChildren().get(i-x);
                if(temp instanceof ParsedObjectLeaf){
                    ParsedObjectLeaf pol=(ParsedObjectLeaf)temp;
                    if(isEndOfSentence(pol)){
                        end=x;
                        break;
                    }else if(isReferenceOperator(pol))
                        lastRef=x; //Last owner is before this
                    
                }else if(temp instanceof ParsedObject){
                    if(temp.getType()==Type.BRACKET)
                        break;
                }
            }
            if(lastRef>0){
                int a=lastRef;
                if((i-lastRef-1)>=0){ //Check if the owner before first :: is function or attribute
                    a++;
                    temp=obj.getChildren().get(i-a);
                    
                    if(temp.getType()==Type.BRACKET){
                        if((i-a-1)>=0){
                            a++;
                        }
                    }                 
                }else{                    
                    //throw new ParseException("Not valid code, expected function call or variable before ::, ->, or .");
                }
               for(;a>1;a--){
                   temp=obj.getChildren().get(i-a);
                   po.addChild(temp);
               }
            }

            return new FunctionCall(po,name,parameters);
        }
        
    }
    
    private static boolean isReferenceOperator(ParsedObjectLeaf leaf){
        String str=leaf.getContent();
        if(str.contentEquals("::"))
            return true;
        else if(str.contentEquals("."))
            return true;
        else if(str.contentEquals("->"))
            return true;
        return false;
    }
    private static boolean isEndOfSentence(ParsedObjectLeaf leaf){
        String str=leaf.getContent();
        if(str.contentEquals(","))
            return true;
        else if(str.contentEquals("="))
            return true;
        else if(str.contentEquals(";"))
            return true;
        return false;
    }
    
    private static int parsePrimitiveVariable(int index, ParsedObject obj,List<Variable> list) {
        System.out.println("primitive variable");
        int x=0, size=obj.getChildren().size();
        Variable var=new Variable();
        var.type=(ParsedObjectLeaf)obj.getChildren().get(index);
        BaseParsedObject o;
        
        x++;
        while((index+x)<size){
            o=obj.getChildren().get(index+x);
            System.out.println("o:"+o.getType());
            if(o.getType()==Type.BRACKET){
                var=null;
                break;
            }else if(o.getType()==Type.SPECIAL){
                if(o.getContent().contentEquals("*")||o.getContent().contentEquals("&"))
                    var.pointers+=o.getContent();
                else if(o.getContent().contentEquals(";")){
                    var=null;
                    break;
                }
            }else if(o.getType()==Type.WORDTOKEN){
                var.name=o.getContent();
                break;
            }
            x++;
        }
        if(var!=null){
            System.out.println("Found primitive variable declaration:"+var.toString());
            //if(var.)
        }
        
        return x;
    }

    private static int parseVariable(int index, ParsedObject obj, List<Variable> list) {
        BaseParsedObject obj1, nextObj;
        int size=obj.getChildren().size()-1;
        if(obj.getType()==Type.BRACKET)
            size--;
        Variable var=new Variable();
        ParsedObject owners=new ParsedObject(null,Type.OTHER,"");
        int x=0;
        //This loop finds the owners(namespace, class, struct...) of variable and it's type eg std::string
        BaseParsedObject temp1=null, temp2;
        for(x=0;(index+x)<size;x++){
            obj1=obj.getChildren().get(index+x);
            nextObj=obj.getChildren().get(index+x+1);
            if(obj1.getType()==Type.WORDTOKEN){
                temp1=obj1;
            }
            if(nextObj.getContent().equals("::")){
                temp2=nextObj;
                x++;
            }else 
                temp2=null;
            if(temp2==null){
                if(temp1==null){
                    var= null;
                    break;
                }
               //System.out.println("Setting type "+temp1.toString());
                var.type=(ParsedObjectLeaf)temp1;
                var.owners=owners;
                break;
            }else{
                String s1="", s2="";
                if(temp1!=null){
                    s1=temp1.toString();
                }
                if(temp2!=null){
                    s2=temp2.toString();
                }
                    
                //System.out.println("Adding "+s1+" "+s2);
                owners.addChild(temp1);
                owners.addChild(temp2);
            }
        }
        x++;
        if(var!=null){
            int template=0;
            String str=null;
            
            //now we check if there is a template after type
        //eg std::vector<std::pair<std::string, int> >
            int i=x;
           while((index+i)<size){
               obj1=obj.getChildren().get(index+i);
               String c=obj1.getContent();
               if(c.contentEquals("<")){
                   template++;
                   if((index+i+1)<size){
                       //If this is found then it means that we are parsing an overloaded function call eg. std::cout<<"hello";
                        //, not a variable of type std::cout
                       if(obj.getChildren().get(index+i+1).getContent().contentEquals("<"))var=null;
                   }
               }
               if(template>0){
                   if(str==null)
                       str="";
                   str+=c;
                   if(c.contentEquals(">"))
                       template--;
               }else break;
               if(index+i+template>=size)
                   var=null;
               i++;
           }
           if(var!=null){
               x=i; //some tokens might have to be parser again if it's found out that we were not parsing a variable
                var.template=str;
                if(str==null)
                    str="null";
                System.out.println("Done parsing template: "+str +" Last token was:"+obj.getChildren().get(index+x).getContent());
                // now we parse the name of the variable
                System.out.println("Found token:"+obj.getChildren().get(index+x).getType()+" "+obj.getChildren().get(index+x).getContent());
                if(obj.getChildren().get(index+x).getType()==Type.WORDTOKEN){
                    var.name=obj.getChildren().get(index+x).getContent();
                    x++;
                    temp1=obj.getChildren().get(index+x);
                    if(temp1.getType()==Type.BRACKET){
                        if(temp1.getContent().contentEquals("("))
                            var=null;
                        else if(temp1.getContent().contentEquals("{"))
                            var=null;
                        if(var==null)
                            x--;
                    }

                }else
                    var=null;
           }
           
        }
        
        
        
        
        if(var!=null)
            System.out.println("Found variable declaration:"+var.toString());
        return x;
        
    }


}
