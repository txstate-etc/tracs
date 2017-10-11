<%-- JAVSCRIPT FOR DISABLING SHOWFEEDBACK AND TOC AFTER ONE CLICK 
Note that this will be embedded in the page exactly as is
--%><!-- Samigo embedded delivery.js starts here -->
<script type="text/javascript">


//added by Amy Boyd for ticket #578
function checkAllPoolQs(btn){
	var elems = btn.form.elements;
	var len = elems.length;
	for (var i=0; i<len; i++) {
		if (elems[i].type && elems[i].type=='checkbox') {
			elems[i].checked=true;
		}
	}
    //enable the 'Copy' button
    updateButtonStatusOnCheck(document.getElementById('editform:import'), document.getElementById('editform'));
	return false;
}

function unCheckAllPoolQs(btn){
	var elems = btn.form.elements;
	var len = elems.length;
	for (var i=0; i<len; i++) {
		if (elems[i].type && elems[i].type=='checkbox') {
			elems[i].checked=false;
		}
	}
    //disable the 'Copy' button
    updateButtonStatusOnCheck(document.getElementById('editform:import'), document.getElementById('editform'));
	return false;
}





function printForm(){
  alert("print 0");
  for(i=0; i<document.forms[0].elements.length; i++)
  {
    alert("The field name is: " + document.forms[0].elements[i].name); 
  }
}

function printLink(){
  for (var i=0; i < document.links.length; i++){
    alert(document.links[i].id);
  }
}

//  show Processing for file upload questions 
//  taking out of deliveryAssessment.jsp, so authoring can use it too.   

function showNotif(item, button,formName)
{

/*
        if (button !="noBlock")
        {
                eval("document." + formName + "." + button + ".disabled=true");
        }
*/
        if (item !== "noNotif") {
                var browserType;
                if (document.all) {browserType = "ie";}
                if (window.navigator.userAgent.toLowerCase().match("gecko")) {browserType= "gecko";}
                if (browserType === "gecko" )
                        document.showItem = eval('document.getElementById(item)');
                else if (browserType === "ie")
                        document.showItem = eval('document.all[item]');
                else
                        document.showItem = eval('document.layers[item]');

                        document.showItem.style.visibility = "visible";
        }

        for (var i=0;i<document.getElementsByTagName("input").length; i++)
        {
                if (document.getElementsByTagName("input").item(i).className == "disableme")
                {
                        document.getElementsByTagName("input").item(i).disabled = "disabled";
                }
        }

}

function showNotif2(item, button,formName)
{
        alert('item: '+item);
        alert('button: '+button);
       alert('formname: '+formName);
        if (button !="noBlock")
        {
                eval("document." + formName + "." + button + ".disabled=true");
        }
        if (item !="noNotif")
        {
                var browserType;
                if (document.all) {browserType = "ie";}
                if (window.navigator.userAgent.toLowerCase().match("gecko")) {browserType= "gecko";}
                if (browserType == "gecko" )
                        document.showItem = eval('document.getElementById(item)');
                else if (browserType == "ie")
                        document.showItem = eval('document.all[item]');
                else
                        document.showItem = eval('document.layers[item]');

                        document.showItem.style.visibility = "visible";
        }

        for (var i=0;i<document.getElementsByTagName("input").length; i++)
        {
                if (document.getElementsByTagName("input").item(i).className == "disableme")
                {
                        document.getElementsByTagName("input").item(i).disabled = "disabled";
                }
        }
}

function clearIfDefaultString(formField, defaultString) {
    if(formField.value == defaultString) {
        formField.value = "";
    }
}

// Removed keyboard watcher that submitted the assessment on the return key - JeffSnider,11/14/08

function show(obj) {
        document.getElementById(obj).style.display = '';
}

function hide(obj) {
        document.getElementById(obj).style.display = 'none';
}

function clickReloadLink(windowToGetFocus){
    

var newindex = 0;
for (i=0; i<document.links.length; i++) {
  if ( document.links[i].id.indexOf("hiddenReloadLink") >=0){
    newindex = i;
    break;
  }
}

document.links[newindex].onclick();
windowToGetFocus.focus();

return false;
}

/* Converts implicit form control labeling to explicit by
 * adding an unique id to form controls if they don't already
 * have one and then setting the corresponding label element's
 * for attribute to form control's id value. This explicit 
 * linkage is better supported by adaptive technologies.
 * See SAK-18851 for original.
 */
fixImplicitLabeling = function(){
  var idCounter = 0;
  $('label select,label input').each(function (idx, oInput) {
    if (!oInput.id) {
       idCounter++;
       $(oInput).attr('id', 'a11yAutoGenInputId' + idCounter.toString());
    }
    if (!$(oInput).parents('label').eq(0).attr('for')) {
       $(oInput).parents('label').eq(0).attr('for', $(oInput).attr('id'));
    }
  });
};


function disableShowTimeWarning() {
	document.getElementById('takeAssessmentForm:showTimeWarning').value = "false";
}
	
</script>
