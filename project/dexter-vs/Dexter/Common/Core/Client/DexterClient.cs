using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Diagnostics;
using Dexter.Common.Defect;
using Dexter.Common.Config.Providers;

namespace Dexter.Common.Client
{
    /// <summary>
    /// Communicates with the dexter server
    /// </summary>
    public class DexterClient : IDexterClient
    {
        IHttpClient httpClient;
        IDexterInfoProvider dexterInfoProvider;
        static IDexterClient instance;
        public static readonly string POST_ANALYSIS_RESULT_V3 = "/api/v3/analysis/result";
        public static readonly string POST_SNAPSHOT_SOURCECODE = "/api/v1/analysis/snapshot/source";

        /// <summary>
        /// get/set a singleton instance of IDexterClient
        /// </summary>
        public static IDexterClient Instance
        {
            get
            {
                if (instance == null)
                    throw new NullReferenceException("No Instance");

                return instance;
            }
            set
            {
                instance = value;
            }
        }

        public DexterClient(IHttpClient httpClient, IDexterInfoProvider dexterInfoProvider)
        {
            this.httpClient = httpClient;
            this.dexterInfoProvider = dexterInfoProvider;
        }

        public Task SendAnalysisResult(string resultJson)
        {
            throw new NotImplementedException();
        }

        /// <summary>
        /// Sends the source code with comments to the dexter server
        /// </summary>
        /// <param name="source">Source code with comments</param>
        public async Task SendSourceCode(SourceCodeJsonFormat source)
        {
            HttpResponseMessage response = await httpClient.PostAsync(POST_SNAPSHOT_SOURCECODE,
                       JsonConvert.SerializeObject(source));

            if (!response.IsSuccessStatusCode.Equals(true))
            {
                Debug.WriteLine(response, "Failed to SendSourceCode");
            }
        }

        /// <summary>
        /// Sends dexter defects to the dexter server
        /// </summary>
        /// <param name="result">Container of dexter defects</param>
        public async Task SendAnalysisResult(DexterResult result)
        {
            var dexterResultString = JsonConvert.SerializeObject(result);

            HttpResponseMessage response = await httpClient.PostAsync(POST_ANALYSIS_RESULT_V3,
                       JsonConvert.SerializeObject(new ResultJsonFormat { Result = dexterResultString }));

            if (!response.IsSuccessStatusCode.Equals(true))
            {
                Debug.WriteLine(response, "Failed to SendAnalysisResult");
            }
        }

        /// <summary>
        /// Returns whether stand-alone mode is checked
        /// </summary>
        public bool IsStandAloneMode()
        {
            return dexterInfoProvider.Load().standalone;
        }
    }

    /// <summary>
    /// JSON Container of dexter defects
    /// </summary>
    public class ResultJsonFormat
    {
        [JsonProperty(PropertyName = "result")]
        public string Result { get; set; }
    }
}
