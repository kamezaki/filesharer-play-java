function browserLanguage() {
  try {
    return (navigator.browserLanguage || navigator.language || navigator.userLanguage).substr(0,2);
  }
  catch(e) {
    return undefined;
  }
}

function toLocalDateTime(epocms, format) {
  var d = moment(epocms);
  var language = browserLanguage();
  if (language != undefined) {
    d.locale(language);
  }
  return d.format(format);
}
